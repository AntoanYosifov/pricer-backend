package com.antdevrealm.pricer_backend.spike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpikePriceObservationRepository extends JpaRepository<SpikePriceObservation, Long> {
}
