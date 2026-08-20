package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/experience")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    public List<ExperienceResponse> getExperience() {
        return experienceService.getExperience();
    }
}
