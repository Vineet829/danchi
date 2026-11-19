package com.danchi.profile.dto;

import com.danchi.rating.Rating;
import java.time.Instant;
import java.util.UUID;

public record ProfileReviewResponse(
        UUID id, int rating, String comment, Instant createdAt,
        String authorName, String authorUsername, String avatar) {

    public static ProfileReviewResponse from(Rating rating, String avatar) {
        return new ProfileReviewResponse(
                rating.getId(), rating.getRating(), rating.getComment(), rating.getCreatedAt(),
                rating.getRatingUser().getFullName(), rating.getRatingUser().getUsername(), avatar);
    }
}
