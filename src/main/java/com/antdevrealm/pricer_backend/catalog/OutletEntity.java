package com.antdevrealm.pricer_backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outlet")
public class OutletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false, updatable = false)
    private RetailerEntity retailer;

    @Column(name = "external_code", nullable = false, updatable = false)
    private String externalCode;

    @Column(nullable = false)
    private String name;

    // No setter: at most one default per retailer (partial unique index in V1), and
    // switching it takes two UPDATEs in a fixed order — a deliberate manual fix, not app code.
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected OutletEntity() {
    }

    public OutletEntity(RetailerEntity retailer, String externalCode, String name, boolean isDefault) {
        this.retailer = Objects.requireNonNull(retailer, "retailer");
        this.externalCode = Objects.requireNonNull(externalCode, "externalCode");
        this.name = Objects.requireNonNull(name, "name");
        this.isDefault = isDefault;
    }

    public UUID getId() {
        return id;
    }

    public RetailerEntity getRetailer() {
        return retailer;
    }

    public String getExternalCode() {
        return externalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
