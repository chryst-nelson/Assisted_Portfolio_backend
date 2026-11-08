package com.cornelius.portfolio.ai;

import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.dto.response.FaqResponse;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.service.AchievementService;
import com.cornelius.portfolio.service.CertificationService;
import com.cornelius.portfolio.service.ContactService;
import com.cornelius.portfolio.service.EducationService;
import com.cornelius.portfolio.service.ExperienceService;
import com.cornelius.portfolio.service.FaqService;
import com.cornelius.portfolio.service.ProfileService;
import com.cornelius.portfolio.service.ProjectService;
import com.cornelius.portfolio.service.ResumeService;
import com.cornelius.portfolio.service.SkillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Holds the current {@link Knowledge} snapshot. It is built once at start-up
 * (in the background), then refreshed in the background whenever it is older
 * than {@code app.chat.knowledge-ttl-seconds}, so a chat request never waits
 * for the database after the first one. {@link #rebuild()} forces an immediate
 * refresh (the admin "Reindex" button).
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    private final ProfileService profileService;
    private final SkillService skillService;
    private final ProjectService projectService;
    private final ExperienceService experienceService;
    private final EducationService educationService;
    private final CertificationService certificationService;
    private final AchievementService achievementService;
    private final ContactService contactService;
    private final ResumeService resumeService;
    private final FaqService faqService;
    private final Duration ttl;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private volatile Knowledge snapshot;

    public KnowledgeBase(
        ProfileService profileService,
        SkillService skillService,
        ProjectService projectService,
        ExperienceService experienceService,
        EducationService educationService,
        CertificationService certificationService,
        AchievementService achievementService,
        ContactService contactService,
        ResumeService resumeService,
        FaqService faqService,
        @Value("${app.chat.knowledge-ttl-seconds:120}") long ttlSeconds
    ) {
        this.profileService = profileService;
        this.skillService = skillService;
        this.projectService = projectService;
        this.experienceService = experienceService;
        this.educationService = educationService;
        this.certificationService = certificationService;
        this.achievementService = achievementService;
        this.contactService = contactService;
        this.resumeService = resumeService;
        this.faqService = faqService;
        this.ttl = Duration.ofSeconds(Math.max(5, ttlSeconds));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        CompletableFuture.runAsync(() -> {
            try {
                rebuild();
            } catch (RuntimeException ex) {
                log.warn("Knowledge warm-up failed; it will be built on the first chat request: {}", ex.getMessage());
            }
        });
    }

    /** The current snapshot; builds it on first use and refreshes it in the background when stale. */
    public Knowledge current() {
        Knowledge k = snapshot;
        if (k == null) {
            return rebuild();
        }
        if (k.builtAt().plus(ttl).isBefore(Instant.now()) && refreshing.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    rebuild();
                } catch (RuntimeException ex) {
                    log.warn("Knowledge refresh failed; serving the previous snapshot: {}", ex.getMessage());
                } finally {
                    refreshing.set(false);
                }
            });
        }
        return k;
    }

    /** Re-reads everything from the database and swaps the snapshot. */
    public synchronized Knowledge rebuild() {
        List<ProjectDetailResponse> projects = new ArrayList<>();
        for (ProjectSummaryResponse summary : projectService.getProjects()) {
            projects.add(projectService.getProjectById(summary.id()));
        }
        List<FaqResponse> faqs = new ArrayList<>();
        for (FaqResponse faq : faqService.getAllForAdmin()) {
            if (faq.enabled()) {
                faqs.add(faq);
            }
        }
        ProfileResponse profile = orNull(profileService::getProfile);
        ContactResponse contact = orNull(contactService::getContact);
        ResumeResponse resume = orNull(resumeService::getActiveResume);
        List<ExperienceResponse> experience = experienceService.getExperience();
        List<EducationResponse> education = educationService.getEducation();
        List<CertificationResponse> certifications = certificationService.getCertifications();
        List<AchievementResponse> achievements = achievementService.getAchievements();

        Knowledge built = new Knowledge(profile, skillService.getSkills(null), projects, experience, education,
            certifications, achievements, contact, resume, faqs);
        this.snapshot = built;
        return built;
    }

    private static <T> T orNull(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NotFoundException ex) {
            return null;
        }
    }
}
