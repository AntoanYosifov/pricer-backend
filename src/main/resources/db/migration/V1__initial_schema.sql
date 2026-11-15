-- =============================================================================
-- V1 — initial schema.
-- Once this runs against the always-on database it is APPEND-ONLY: never edit
-- it, add V2. Postgres DDL is transactional — this file applies whole or not at all.
--
-- Conventions:
--   ids        uuid, DEFAULT gen_random_uuid() — any writer (JPA, native upsert,
--              psql) gets one without remembering to supply it.
--   enums      text + CHECK, never CREATE TYPE … AS ENUM.
--   money      numeric(10,2). Derived unit prices numeric(12,4).
--   time       timestamptz for instants (stored UTC); date for business days.
--   deletes    no ON DELETE CASCADE anywhere. price_observation can never be
--              rebuilt; nothing may delete it as a side effect.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- retailer — one row per chain. Seeded, rarely changes.
-- -----------------------------------------------------------------------------
CREATE TABLE retailer
(
    id          uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    code        text        NOT NULL, -- 'spar', 'billa' — matches the seed YAML keys
    name        text        NOT NULL, -- 'SPAR', 'BILLA'
    website_url text        NOT NULL, -- link-back target (CLAUDE.md §4)
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_retailer_code UNIQUE (code)
);

-- -----------------------------------------------------------------------------
-- outlet — where a price is valid. A property of the PRICE, not the product:
-- Spar's catalog route takes no outlet, its price route does.
--   Spar  → '8999' (salesChannel NATIONAL, a pseudo-store)
--   Billa → one row for its online price list
-- -----------------------------------------------------------------------------

CREATE TABLE outlet
(

    id            uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    retailer_id   uuid        NOT NULL REFERENCES retailer (id),
    external_code text        NOT NULL, -- the value sent to the source API
    name          text        NOT NULL,
    is_default    boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_outlet_retailer_code UNIQUE (retailer_id, external_code)
);

-- At most ONE default outlet per retailer — what "today's Spar price" reads from.
-- A partial unique index: uniqueness applies only to rows matching the WHERE.
CREATE UNIQUE INDEX uq_outlet_one_default_per_retailer
    ON outlet (retailer_id) WHERE is_default;

-- -----------------------------------------------------------------------------
-- category — OUR canonical taxonomy (seed slugs), not any retailer's.
-- -----------------------------------------------------------------------------
CREATE TABLE category
(

    id         uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    slug       text        NOT NULL, -- 'butter', 'milch' — lowercase ASCII
    name       text,                 -- display name; seed only gives slugs today
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_category_slug UNIQUE (slug)
);

-- -----------------------------------------------------------------------------
-- product — the canonical staple ("Butter"). No pack size: that is per retailer.
-- -----------------------------------------------------------------------------
CREATE TABLE product
(

    id          uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    slug        text        NOT NULL, -- seed key, ends up in URLs
    name        text        NOT NULL, -- 'Vollmilch 3,5%' — no pack size
    category_id uuid        NOT NULL REFERENCES category (id),
    unit_type   text        NOT NULL, -- what "per unit" means for this product
    ean         text,                 -- Spar has none; Billa may
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_slug UNIQUE (slug),
    -- Plain UNIQUE: NULLs are distinct, so many NULLs, no duplicate values.
    -- NOT "NULLS NOT DISTINCT" — that would allow only one NULL.
    CONSTRAINT uq_product_ean UNIQUE (ean),
    CONSTRAINT
        ck_product_unit_type
        CHECK (
            unit_type
                IN
            (
             'WEIGHT',
             'VOLUME',
             'PIECE'
                ))
);

-- -----------------------------------------------------------------------------
-- retailer_product — one retailer's SKU for a canonical product.
-- Seeded with (retailer, product, external_id, url); the weekly catalog refresh
-- fills the rest — hence the nullable catalog columns.
-- -----------------------------------------------------------------------------

CREATE TABLE retailer_product
(

    id                   uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    retailer_id          uuid        NOT NULL REFERENCES retailer (id),
    product_id           uuid        NOT NULL REFERENCES product (id),
    external_id          text        NOT NULL, -- Spar articleId: OPAQUE, 7 or 13 digits
    url                  text        NOT NULL, -- canonical product page (link-back)

    -- filled by the catalog refresh
    name                 text,                 -- 'Österreichische Teebutter'
    brand                text,                 -- 'S-BUDGET'
    pack_size            numeric(10, 3),       -- 0.250  (as the source states it)
    pack_unit            text,                 -- 'KG'   (as the source states it; no CHECK —
    --         we don't know every retailer's set)
    base_factor          numeric(10, 4),       -- pack in kg / l / pieces: 0.2500
    catalog_refreshed_at timestamptz,          -- NULL = catalog never fetched yet

    created_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_retailer_product_external UNIQUE (retailer_id, external_id),
    CONSTRAINT ck_retailer_product_pack_size CHECK (pack_size > 0),
    CONSTRAINT ck_retailer_product_base_factor CHECK (base_factor > 0)
);

-- "All retailer SKUs for this product" — the product page query.
-- (Postgres does NOT index foreign keys automatically.)
CREATE INDEX ix_retailer_product_product ON retailer_product (product_id);

-- -----------------------------------------------------------------------------
-- ingestion_run — one row per job execution. The guard against silent failure.
-- -----------------------------------------------------------------------------
CREATE TABLE ingestion_run
(

    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    retailer_id            uuid        NOT NULL REFERENCES retailer (id),
    kind                   text        NOT NULL, -- catalog weekly, price daily
    status                 text        NOT NULL,
    started_at             timestamptz NOT NULL,
    finished_at            timestamptz,          -- NULL while RUNNING (or if it crashed)
    items_seen             integer,
    items_written          integer,
    min_remaining_requests integer,              -- Spar's x-remaining-requests, lowest seen
    error                  text,

    CONSTRAINT ck_ingestion_run_kind CHECK (kind IN ('CATALOG', 'PRICE')),
    CONSTRAINT ck_ingestion_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

-- Volume-drop check: "latest successful run of this kind for this retailer".
CREATE INDEX ix_ingestion_run_lookup ON ingestion_run (retailer_id, kind, started_at DESC);

-- -----------------------------------------------------------------------------
-- price_observation — THE table. One row per SKU, per outlet, per business day.
-- Can never be rebuilt. Written only by a native INSERT … ON CONFLICT upsert.
-- ---------------------------------------------------------------------------

CREATE TABLE price_observation
(

    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    retailer_product_id    uuid        NOT NULL REFERENCES retailer_product (id),
    outlet_id              uuid        NOT NULL REFERENCES outlet (id),
    observed_on            date        NOT NULL,               -- business day, Europe/Vienna, from our Clock
    fetched_at             timestamptz NOT NULL,               -- provenance: the actual instant
    ingestion_run_id       uuid REFERENCES ingestion_run (id), -- last run that wrote it
    source_price_date      date,                               -- Spar's forDate — cross-check only

    status                 text        NOT NULL,

    -- prices (excluding deposit / Pfand — every adapter)
    price                  numeric(10, 2),                     -- calculatedPrice: what you pay
    regular_price          numeric(10, 2),                     -- stattPrice ?? calculatedPrice
    omnibus_price          numeric(10, 2),                     -- retailer-reported 30-day low

    -- unit price: ours vs the source's (mismatch = parser alarm)
    unit_price             numeric(12, 4),                     -- computed by us: price / base_factor
    source_unit_price      numeric(12, 4),                     -- comparisonPrice.price  (absent for loose goods)
    source_unit_quantity   numeric(10, 3),                     -- comparisonPrice.quantity
    source_unit            text,                               -- comparisonPrice.unit

    -- promo facts: one column per source flag. NULL = retailer doesn't expose it.
    on_promotion           boolean,                            -- inAngebot            temporary
    has_promotion_price    boolean,                            -- hasAktionsPrice      temporary
    everyday_low_price     boolean,                            -- immerBillig          permanent
    price_reduced          boolean,                            -- isPreisGesenkt       structural
    price_reduced_since    date,                               -- preisGesenktAb       structural
    price_reduction_reason text,                               -- preisGesenktType, raw; NULL if not reduced
    multibuy_discount      boolean,                            -- mengenRabatt         conditional
    percentage_discount    boolean,                            -- prozentRabatt        conditional
    app_voucher            boolean,                            -- hasAppVoucher        conditional

    -- The idempotency target, named in the upsert's ON CONFLICT clause.
    -- Also serves the history query (product, outlet, date range) — no extra index.
    CONSTRAINT uq_price_observation_day
        UNIQUE (retailer_product_id, outlet_id, observed_on),

    CONSTRAINT ck_price_observation_status
        CHECK (status IN ('OK', 'UNAVAILABLE')),

    -- A gap is not a zero: OK ⇔ has a price, UNAVAILABLE ⇔ has none.
    CONSTRAINT ck_price_observation_status_price CHECK ( (status = 'OK' AND price IS NOT NULL) OR
                                                         (status = 'UNAVAILABLE' AND price IS NULL) ),

    CONSTRAINT ck_price_observation_non_negative CHECK ( (price IS NULL OR price >= 0) AND
                                                         (regular_price IS NULL OR regular_price >= 0) AND
                                                         (omnibus_price IS NULL OR omnibus_price >= 0) )
);

-- "Everything the PRICE run wrote" — debugging a bad day.
CREATE INDEX ix_price_observation_run ON price_observation (ingestion_run_id);