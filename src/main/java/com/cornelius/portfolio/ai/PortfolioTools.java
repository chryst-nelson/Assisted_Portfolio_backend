package com.cornelius.portfolio.ai;

import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.dto.response.SkillResponse;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.service.AchievementService;
import com.cornelius.portfolio.service.CertificationService;
import com.cornelius.portfolio.service.ContactService;
import com.cornelius.portfolio.service.EducationService;
import com.cornelius.portfolio.service.ExperienceService;
import com.cornelius.portfolio.service.ProfileService;
import com.cornelius.portfolio.service.ProjectService;
import com.cornelius.portfolio.service.ResumeService;
import com.cornelius.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Every tool wraps an existing public-read service from M1 — same data,
 * same DTOs, same guarantee: whatever the assistant says came verbatim from
 * the database, never from the model retyping it.
 *
 * Card-worthy results (projects, skills, experience, contact, resume) are
 * also handed to ToolCallRecorder so ChatOrchestrationService can attach the
 * raw data to the API response. Profile, education, certifications, and
 * achievements aren't dedicated card types in the plan's response contract —
 * they ground the model's prose answer but don't get a payload.
 */
@Component
@RequiredArgsConstructor
public class PortfolioTools {

    private final ProfileService profileService;
    private final SkillService skillService;
    private final ProjectService projectService;
    private final ExperienceService experienceService;
    private final EducationService educationService;
    private final CertificationService certificationService;
    private final AchievementService achievementService;
    private final ContactService contactService;
    private final ResumeService resumeService;
    private final ToolCallRecorder toolCallRecorder;

    @Tool(description = "Get Cornelius's profile: full name, title, bio, location, interests, and career goals. Use this for general 'who is Cornelius' or background questions.")
    public Object getProfile() {
        try {
            return profileService.getProfile();
        } catch (NotFoundException ex) {
            return ex.getMessage();
        }
    }

    @Tool(description = "Get Cornelius's skills, optionally filtered by category (e.g. 'Backend', 'Frontend', 'DevOps'). Use this when asked what technologies he knows or works with.")
    public List<SkillResponse> getSkills(
        @ToolParam(description = "Optional category filter. Omit to get all skills.", required = false)
        String category
    ) {
        List<SkillResponse> skills = skillService.getSkills(category);
        toolCallRecorder.record(ChatResponseType.SKILL_LIST, skills);
        return skills;
    }

    @Tool(description = "Get a summary list of all of Cornelius's projects. Use this when asked to list, browse, or show his projects generally.")
    public List<ProjectSummaryResponse> getProjects() {
        List<ProjectSummaryResponse> projects = projectService.getProjects();
        toolCallRecorder.record(ChatResponseType.PROJECT_LIST, projects);
        return projects;
    }

    @Tool(description = "Get full detail on one specific project by its numeric ID, including technologies, screenshots, and challenges. Call getProjects or searchProjects first if you don't already know the ID.")
    public ProjectDetailResponse getProjectById(
        @ToolParam(description = "The project's numeric ID")
        Long id
    ) {
        ProjectDetailResponse project = projectService.getProjectById(id);
        toolCallRecorder.record(ChatResponseType.PROJECT, project);
        return project;
    }

    @Tool(description = "Search Cornelius's projects by technology name (e.g. 'React', 'Spring Boot', 'Docker'). Use this when asked to show projects using a specific technology.")
    public List<ProjectSummaryResponse> searchProjects(
        @ToolParam(description = "The technology to search for")
        String technology
    ) {
        List<ProjectSummaryResponse> projects = projectService.searchByTechnology(technology);
        toolCallRecorder.record(ChatResponseType.PROJECT_LIST, projects);
        return projects;
    }

    @Tool(description = "Get Cornelius's work experience: companies, roles, dates, responsibilities, technologies used, and achievements. Use this for questions about his professional history.")
    public List<ExperienceResponse> getExperience() {
        List<ExperienceResponse> experience = experienceService.getExperience();
        toolCallRecorder.record(ChatResponseType.EXPERIENCE, experience);
        return experience;
    }

    @Tool(description = "Get Cornelius's education history: institutions, degrees, courses. Use this for questions about his academic background.")
    public List<EducationResponse> getEducation() {
        return educationService.getEducation();
    }

    @Tool(description = "Get Cornelius's certifications: name, provider, issue date. Use this for questions about his certifications or credentials.")
    public List<CertificationResponse> getCertifications() {
        return certificationService.getCertifications();
    }

    @Tool(description = "Get Cornelius's verified achievements. Use this for questions about awards, recognitions, or notable accomplishments.")
    public List<AchievementResponse> getAchievements() {
        return achievementService.getAchievements();
    }

    @Tool(description = "Get Cornelius's contact information: email, LinkedIn, GitHub, and other links. Use this when a visitor asks how to reach or contact him.")
    public Object getContactInformation() {
        try {
            ContactResponse contact = contactService.getContact();
            toolCallRecorder.record(ChatResponseType.CONTACT, contact);
            return contact;
        } catch (NotFoundException ex) {
            return ex.getMessage();
        }
    }

    @Tool(description = "Get Cornelius's currently active resume file: a download URL and version label. Use this when a visitor asks to download or see his resume or CV.")
    public Object getResume() {
        try {
            ResumeResponse resume = resumeService.getActiveResume();
            toolCallRecorder.record(ChatResponseType.RESUME, resume);
            return resume;
        } catch (NotFoundException ex) {
            return ex.getMessage();
        }
    }
}
