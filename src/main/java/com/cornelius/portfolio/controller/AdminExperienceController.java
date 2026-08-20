package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.ExperienceRequest;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/experience")
@RequiredArgsConstructor
public class AdminExperienceController {

    private final ExperienceService experienceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceResponse create(@Valid @RequestBody ExperienceRequest request) {
        return experienceService.createExperience(request);
    }

    @PutMapping("/{id}")
    public ExperienceResponse update(@PathVariable Long id, @Valid @RequestBody ExperienceRequest request) {
        return experienceService.updateExperience(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        experienceService.deleteExperience(id);
    }
}
