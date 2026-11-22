package com.danchi.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CountriesTest {

    @Test
    void acceptsOnlyKnownCodes() {
        assertThat(Countries.nameOf("in")).isEqualTo("India");
        for (String value : List.of("zz", "ZZ", "abc!", "123456", "")) {
            assertThat(Countries.nameOf(value)).isEmpty();
            assertThat(Countries.codeOf(value)).isEmpty();
        }
    }
}
