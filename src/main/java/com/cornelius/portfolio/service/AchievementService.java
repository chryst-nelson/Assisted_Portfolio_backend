package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.AchievementRequest;
import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.entity.Achievement;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    // Public/chat-facing — verified only. This is the data-integrity rule
    // from the plan ("verified achievements only") enforced at the query
    // level, not just convention.
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements() {
        return achievementRepository.findByVerifiedTrueOrderByDateDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    // Admin-facing — sees everything, including unverified entries, so the
    // owner can review and verify pending ones.
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievementsForAdmin() {
        return achievementRepository.findAllByOrderByDateDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AchievementResponse createAchievement(AchievementRequest request) {
        Achievement achievement = Achievement.builder()
            .title(request.title())
            .description(request.description())
            .date(request.date())
            .verified(request.verified())
            .build();
        return toResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public AchievementResponse updateAchievement(Long id, AchievementRequest request) {
        Achievement achievement = achievementRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No achievement found with id " + id));

        achievement.setTitle(request.title());
        achievement.setDescription(request.description());
        achievement.setDate(request.date());
        achievement.setVerified(request.verified());

        return toResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public void deleteAchievement(Long id) {
        if (!achievementRepository.existsById(id)) {
            throw new NotFoundException("No achievement found with id " + id);
        }
        achievementRepository.deleteById(id);
    }

    private AchievementResponse toResponse(Achievement a) {
        return new AchievementResponse(a.getId(), a.getTitle(), a.getDescription(), a.getDate(), a.isVerified());
    }
}
