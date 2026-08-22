package com.antdevrealm.pricer_backend.spike.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ComparisonPrice(
        BigDecimal price,
        BigDecimal quantity,
        String unit) {
    public ComparisonPrice {
        price = price == null ? null : price.setScale(2, RoundingMode.HALF_UP);
    }
}
