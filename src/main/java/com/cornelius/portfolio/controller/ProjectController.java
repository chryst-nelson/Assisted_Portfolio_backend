package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectSummaryResponse> getProjects() {
        return projectService.getProjects();
    }

    @GetMapping("/search")
    public List<ProjectSummaryResponse> searchProjects(@RequestParam String tech) {
        return projectService.searchByTechnology(tech);
    }

    @GetMapping("/{id}")
    public ProjectDetailResponse getProject(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }
}
