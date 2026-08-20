package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.ProjectRequest;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{id}")
    public ProjectDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}
