package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.ProjectRequest;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.entity.Project;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.mapper.ProjectMapper;
import com.cornelius.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getProjects() {
        return projectMapper.toSummaryList(projectRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No project found with id " + id));
        return projectMapper.toDetail(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> searchByTechnology(String tech) {
        return projectMapper.toSummaryList(projectRepository.searchByTechnology(tech));
    }

    @Transactional
    public ProjectDetailResponse createProject(ProjectRequest request) {
        Project project = projectMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return projectMapper.toDetail(projectRepository.save(project));
    }

    @Transactional
    public ProjectDetailResponse updateProject(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No project found with id " + id));
        projectMapper.updateEntityFromRequest(request, project);
        project.setUpdatedAt(LocalDateTime.now());
        return projectMapper.toDetail(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new NotFoundException("No project found with id " + id);
        }
        projectRepository.deleteById(id);
    }
}
