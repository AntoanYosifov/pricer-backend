package com.antdevrealm.pricer_backend.spike.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record SparPriceResponse(
    String articleId,
    BigDecimal calculatedPrice,
    LocalDate forDate,
    ComparisonPrice comparisonPrice
) {
    public SparPriceResponse {
        calculatedPrice = calculatedPrice == null ? null : calculatedPrice.setScale(2, RoundingMode.UNNECESSARY);
    }
}
