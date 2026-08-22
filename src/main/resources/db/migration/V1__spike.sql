create table spike_price_observation
(
    id          bigint generated always as identity primary key,
    external_id text       not null,
    outlet      text       not null,
    observed_on date       not null,
    fetched_at  timestamptz not null,
    price       numeric(10, 2),
    unit_price  numeric(10, 2),
    constraint uq_spike_observation unique (external_id, outlet, observed_on)
);