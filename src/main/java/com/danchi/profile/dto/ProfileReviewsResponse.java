package com.danchi.profile.dto;

import com.danchi.common.PageResponse;
import java.util.List;

public record ProfileReviewsResponse(
        double averageRating, List<RatingBucket> distribution,
        PageResponse<ProfileReviewResponse> reviews) {

    public record RatingBucket(int stars, long count) {}
}
