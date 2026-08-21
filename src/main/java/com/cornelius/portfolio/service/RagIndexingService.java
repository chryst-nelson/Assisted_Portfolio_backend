package com.cornelius.portfolio.service;

import com.cornelius.portfolio.entity.Experience;
import com.cornelius.portfolio.entity.Profile;
import com.cornelius.portfolio.entity.Project;
import com.cornelius.portfolio.repository.ExperienceRepository;
import com.cornelius.portfolio.repository.ProfileRepository;
import com.cornelius.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rebuilds the vector store from scratch on every run — the simplest
 * correct behavior for a portfolio-scale corpus (a handful of projects and
 * experience entries, not thousands of documents). Triggered manually via
 * POST /api/admin/reindex; there's deliberately no automatic per-save hook
 * on ProjectService/ExperienceService/ProfileService, to keep AI concerns
 * out of the core CRUD services.
 *
 * Chunked entities: profile, projects, experience — the free-text content
 * that benefits from semantic matching. Skills/education/certifications/
 * achievements are structured enough that M6's tool-calling already serves
 * them well; RAG earns its keep specifically where a question doesn't map
 * to a clean structured query (see KnowledgeBaseTools).
 */
@Service
@RequiredArgsConstructor
public class RagIndexingService {

    private final ProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int reindex() {
        // The vector_store table only ever holds this project's RAG content,
        // so a full wipe before rebuilding is safe and simplest — no need to
        // track which chunks are stale versus current.
        jdbcTemplate.update("DELETE FROM vector_store");

        List<Document> documents = new ArrayList<>();
        profileRepository.findTopByOrderByIdAsc().ifPresent(profile -> documents.add(toDocument(profile)));
        projectRepository.findAll().forEach(project -> documents.add(toDocument(project)));
        experienceRepository.findAll().forEach(experience -> documents.add(toDocument(experience)));

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        return documents.size();
    }

    private Document toDocument(Profile profile) {
        String content = "Profile: %s, %s. %s Interests: %s. Career goals: %s.".formatted(
                profile.getFullName(),
                nullToEmpty(profile.getTitle()),
                nullToEmpty(profile.getBio()),
                nullToEmpty(profile.getInterests()),
                nullToEmpty(profile.getCareerGoals())
        );

        return new Document(UUID.randomUUID().toString(), content,
                Map.of("sourceTable", "profiles", "sourceId", profile.getId()));
    }

    private Document toDocument(Project project) {
        String technologies = project.getTechnologies().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.joining(", "));

        String challenges = project.getChallenges().stream()
                .map(c -> c.getLessonLearned() != null
                        ? c.getChallenge() + " Lesson: " + c.getLessonLearned()
                        : c.getChallenge())
                .collect(Collectors.joining(" "));

        String content = "Project: %s. %s Problem solved: %s Technologies: %s. Challenges: %s".formatted(
                project.getName(),
                nullToEmpty(project.getDescription()),
                nullToEmpty(project.getProblemSolved()),
                technologies,
                challenges
        );

        return new Document(UUID.randomUUID().toString(), content,
                Map.of("sourceTable", "projects", "sourceId", project.getId()));
    }

    private Document toDocument(Experience experience) {
        String technologies = experience.getTechnologies().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.joining(", "));

        String achievements = experience.getAchievements().stream()
                .map(a -> a.getAchievement())
                .collect(Collectors.joining(" "));

        String endDate = experience.getEndDate() != null ? experience.getEndDate().toString() : "Present";

        String content = "Experience: %s at %s (%s - %s). %s Technologies: %s. Achievements: %s".formatted(
                experience.getRole(),
                experience.getCompany(),
                experience.getStartDate(),
                endDate,
                nullToEmpty(experience.getResponsibilities()),
                technologies,
                achievements
        );

        return new Document(UUID.randomUUID().toString(), content,
                Map.of("sourceTable", "experiences", "sourceId", experience.getId()));
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}