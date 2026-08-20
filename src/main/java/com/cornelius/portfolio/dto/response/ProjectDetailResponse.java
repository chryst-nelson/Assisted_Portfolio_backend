package com.cornelius.portfolio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponse(
    Long id,
    String name,
    String slug,
    String description,
    String problemSolved,
    String category,
    String githubUrl,
    String liveUrl,
    List<SkillResponse> technologies,
    List<ProjectScreenshotResponse> screenshots,
    List<ProjectChallengeResponse> challenges,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
