package com.antdevrealm.pricer_backend.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OutletEntityTest {

    private final RetailerEntity spar = new RetailerEntity("spar", "SPAR", "https://www.spar.at");

    @Test
    void constructorRejectsNullRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> new OutletEntity(null, "8999", "National", true)).withMessage("retailer");
        assertThatNullPointerException().isThrownBy(() -> new OutletEntity(spar, null, "National", true)).withMessage("externalCode");
        assertThatNullPointerException().isThrownBy(() -> new OutletEntity(spar, "8999", null, true)).withMessage("name");
    }

    @Test
    void setNameRejectsNull() {
        OutletEntity national = new OutletEntity(spar, "8999", "National", true);

        assertThatNullPointerException().isThrownBy(() -> national.setName(null)).withMessage("name");
    }
}
