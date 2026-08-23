package com.antdevrealm.pricer_backend.spike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Repository
public interface SpikePriceObservationRepository extends JpaRepository<SpikePriceObservation, Long> {
    @Modifying
    @Transactional
    @Query(value = """
            insert into spike_price_observation
                        (external_id, outlet, observed_on, fetched_at, price, unit_price)
                        values (:externalId, :outlet, :observedOn, :fetchedAt, :price, :unitPrice)
                        on conflict (external_id, outlet, observed_on) do update set
                        fetched_at = excluded.fetched_at,
                        price = excluded.price,
                        unit_price = excluded.unit_price
            """, nativeQuery = true)
    int upsert(String externalId, String outlet, LocalDate observedOn,
               Instant fetchedAt, BigDecimal price, BigDecimal unitPrice);
}
