package com.antdevrealm.pricer_backend.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CategoryEntityTest {

    @Test
    void constructorRejectsNullSlug() {
        assertThatNullPointerException().isThrownBy(() -> new CategoryEntity(null)).withMessage("slug");
    }

    @Test
    void nameIsNullable() {
        CategoryEntity butter = new CategoryEntity("butter");
        butter.setName(null);

        assertThat(butter.getName()).isNull();
    }
}
