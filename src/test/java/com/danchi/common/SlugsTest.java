package com.danchi.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void fallbackForEmptySlug() {
        String hindi = "नमस्ते";
        assertThat(Slugs.of(hindi)).isEmpty();
        assertThat(Slugs.of(hindi, "post")).isEqualTo("post");
        assertThat(Slugs.of("!!!", "tag")).isEqualTo("tag");
        assertThat(Slugs.of("Café opening hours", "post")).isEqualTo("cafe-opening-hours");
        assertThat(Slugs.of("Water " + hindi + " tank", "post")).isEqualTo("water-tank");
    }
}
