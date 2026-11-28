package com.antdevrealm.pricer_backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retailer")
public class RetailerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "website_url", nullable = false)
    private String websiteUrl;

    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected RetailerEntity() {
    }

    public RetailerEntity(String code, String name, String websiteUrl) {
        this.code = code;
        this.name = name;
        this.websiteUrl = websiteUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
