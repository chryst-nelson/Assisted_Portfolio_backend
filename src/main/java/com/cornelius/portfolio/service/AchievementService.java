package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements() {
        return achievementRepository.findByVerifiedTrueOrderByDateDesc().stream()
            .map(a -> new AchievementResponse(a.getId(), a.getTitle(), a.getDescription(), a.getDate()))
            .toList();
    }
}
