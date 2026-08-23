package com.antdevrealm.pricer_backend.spike;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Table(
        name = "spike_price_observation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_spike_observation",
                columnNames = {"external_id", "outlet", "observed_on"}
        )
)
@Entity
public class SpikePriceObservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String outlet;

    @Column(nullable = false)
    private LocalDate observedOn;

    @Column(nullable = false)
    private Instant fetchedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected SpikePriceObservation() {
    }

    public SpikePriceObservation(
            String externalId,
            String outlet,
            LocalDate observedOn,
            Instant fetchedAt,
            BigDecimal price,
            BigDecimal unitPrice) {
        this.externalId = externalId;
        this.outlet = outlet;
        this.observedOn = observedOn;
        this.fetchedAt = fetchedAt;
        this.price = price;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getOutlet() {
        return outlet;
    }

    public LocalDate getObservedOn() {
        return observedOn;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Override
    public String toString() {
        return "SpikePriceObservation{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", outlet='" + outlet + '\'' +
                ", observedOn=" + observedOn +
                ", fetchedAt=" + fetchedAt +
                ", price=" + price +
                ", unitPrice=" + unitPrice +
                '}';
    }
}


