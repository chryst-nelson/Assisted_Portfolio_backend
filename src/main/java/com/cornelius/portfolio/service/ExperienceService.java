package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.ExperienceRequest;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.entity.Experience;
import com.cornelius.portfolio.entity.ExperienceAchievement;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.mapper.ExperienceMapper;
import com.cornelius.portfolio.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final ExperienceMapper experienceMapper;

    @Transactional(readOnly = true)
    public List<ExperienceResponse> getExperience() {
        return experienceMapper.toResponseList(experienceRepository.findAllByOrderByStartDateDesc());
    }

    @Transactional
    public ExperienceResponse createExperience(ExperienceRequest request) {
        Experience experience = experienceMapper.toEntity(request);
        experience.setCreatedAt(LocalDateTime.now());
        applyAchievements(experience, request.achievements());
        return experienceMapper.toResponse(experienceRepository.save(experience));
    }

    @Transactional
    public ExperienceResponse updateExperience(Long id, ExperienceRequest request) {
        Experience experience = experienceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No experience found with id " + id));
        experienceMapper.updateEntityFromRequest(request, experience);
        applyAchievements(experience, request.achievements());
        return experienceMapper.toResponse(experienceRepository.save(experience));
    }

    @Transactional
    public void deleteExperience(Long id) {
        if (!experienceRepository.existsById(id)) {
            throw new NotFoundException("No experience found with id " + id);
        }
        experienceRepository.deleteById(id);
    }

    // Achievements are an owned, cascade-managed collection — clearing +
    // re-adding relies on orphanRemoval to delete rows dropped on update.
    private void applyAchievements(Experience experience, List<String> achievements) {
        experience.getAchievements().clear();
        if (achievements == null) {
            return;
        }
        List<ExperienceAchievement> entities = new ArrayList<>();
        for (String text : achievements) {
            entities.add(ExperienceAchievement.builder()
                .experience(experience)
                .achievement(text)
                .build());
        }
        experience.getAchievements().addAll(entities);
    }
}
