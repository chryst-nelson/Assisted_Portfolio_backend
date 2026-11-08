package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.AchievementRequest;
import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/achievements")
@RequiredArgsConstructor
public class AdminAchievementController {

    private final AchievementService achievementService;

    // Admin-only read: sees unverified entries too, unlike the public
    // GET /api/achievements (which only ever returns verified ones).
    @GetMapping
    public List<AchievementResponse> getAll() {
        return achievementService.getAllAchievementsForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AchievementResponse create(@Valid @RequestBody AchievementRequest request) {
        return achievementService.createAchievement(request);
    }

    @PutMapping("/{id}")
    public AchievementResponse update(@PathVariable Long id, @Valid @RequestBody AchievementRequest request) {
        return achievementService.updateAchievement(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        achievementService.deleteAchievement(id);
    }
}
