package com.danchi.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void usesServerPageSize() {
        var response = PageResponse.from(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7), "https://example/posts/?search=x");
        assertThat(response.pageSize()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.next()).endsWith("&page=3");
        assertThat(response.previous()).isEqualTo("https://example/posts/?search=x");
    }
}
