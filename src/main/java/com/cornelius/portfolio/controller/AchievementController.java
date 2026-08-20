package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public List<AchievementResponse> getAchievements() {
        return achievementService.getAchievements();
    }
}
