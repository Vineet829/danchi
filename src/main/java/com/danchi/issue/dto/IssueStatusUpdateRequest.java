package com.danchi.issue.dto;

import com.danchi.issue.IssueStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IssueStatusUpdateRequest(
        @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String title,
        @Pattern(regexp = "(?s).*\\S.*", message = "This field may not be blank.") String description,
        IssueStatus status) {}
