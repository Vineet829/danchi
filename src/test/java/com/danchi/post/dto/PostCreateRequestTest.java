package com.danchi.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PostCreateRequestTest {

    @Test
    void allowsAtMostTenTags() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var tags = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();
        assertThat(validator.validate(new PostCreateRequest("Title", "Body", tags)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("tags");
        assertThat(validator.validate(new PostCreateRequest("Title", "Body", tags.subList(0, 10)))).isEmpty();
    }
}
