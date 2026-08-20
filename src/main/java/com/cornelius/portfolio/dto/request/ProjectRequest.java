package com.cornelius.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProjectRequest(
    @NotBlank String name,
    @NotBlank String slug,
    String description,
    String problemSolved,
    String category,
    String githubUrl,
    String liveUrl,
    List<Long> technologyIds
) {}
