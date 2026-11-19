package com.danchi.profile;

import com.danchi.common.ObjectLabel;
import com.danchi.common.PageResponse;
import com.danchi.profile.dto.ProfilePostResponse;
import com.danchi.profile.dto.ProfileReviewsResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/profiles/{slug}")
public class ProfileActivityController {

    private final ProfileActivityService activity;

    public ProfileActivityController(ProfileActivityService activity) {
        this.activity = activity;
    }

    @GetMapping("/posts/")
    @ObjectLabel("profile_posts")
    public PageResponse<ProfilePostResponse> posts(
            @PathVariable String slug,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "3") int pageSize) {
        return PageResponse.from(activity.posts(slug, pageRequest(page, pageSize)), pageUrl());
    }

    @GetMapping("/reviews/")
    @ObjectLabel("profile_reviews")
    public ProfileReviewsResponse reviews(
            @PathVariable String slug,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "3") int pageSize) {
        return activity.reviews(slug, pageRequest(page, pageSize), pageUrl());
    }

    private PageRequest pageRequest(int page, int pageSize) {
        return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(pageSize, 1), 20));
    }

    private String pageUrl() {
        return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page")
                .build().toUriString();
    }
}
