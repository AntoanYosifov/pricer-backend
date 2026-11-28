package com.antdevrealm.pricer_backend.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RetailerEntityTest {

    @Test
    void constructorRejectsNullRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> new RetailerEntity(null, "SPAR", "https://www.spar.at")).withMessage("code");
        assertThatNullPointerException().isThrownBy(() -> new RetailerEntity("spar", null, "https://www.spar.at")).withMessage("name");
        assertThatNullPointerException().isThrownBy(() -> new RetailerEntity("spar", "SPAR", null)).withMessage("websiteUrl");
    }

    @Test
    void settersRejectNull() {
        RetailerEntity spar = new RetailerEntity("spar", "SPAR", "https://www.spar.at");

        assertThatNullPointerException().isThrownBy(() -> spar.setName(null)).withMessage("name");
        assertThatNullPointerException().isThrownBy(() -> spar.setWebsiteUrl(null)).withMessage("websiteUrl");
    }
}
