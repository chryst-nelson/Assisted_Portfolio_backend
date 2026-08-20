package com.cornelius.portfolio.mapper;

import com.cornelius.portfolio.dto.request.ProjectRequest;
import com.cornelius.portfolio.dto.response.ProjectChallengeResponse;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectScreenshotResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.entity.Project;
import com.cornelius.portfolio.entity.ProjectChallenge;
import com.cornelius.portfolio.entity.ProjectScreenshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = { SkillMapper.class, SkillResolver.class })
public interface ProjectMapper {

    ProjectSummaryResponse toSummary(Project project);

    List<ProjectSummaryResponse> toSummaryList(List<Project> projects);

    ProjectDetailResponse toDetail(Project project);

    ProjectScreenshotResponse toScreenshotResponse(ProjectScreenshot screenshot);

    ProjectChallengeResponse toChallengeResponse(ProjectChallenge challenge);

    // Screenshots and challenges are managed separately (not part of this MVP's
    // admin CRUD scope) — request-side mapping only touches top-level fields
    // and the technology associations.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "technologies", source = "technologyIds")
    @Mapping(target = "screenshots", ignore = true)
    @Mapping(target = "challenges", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toEntity(ProjectRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "technologies", source = "technologyIds")
    @Mapping(target = "screenshots", ignore = true)
    @Mapping(target = "challenges", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ProjectRequest request, @MappingTarget Project project);
}
