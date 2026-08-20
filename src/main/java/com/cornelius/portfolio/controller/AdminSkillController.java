package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.SkillRequest;
import com.cornelius.portfolio.dto.response.SkillResponse;
import com.cornelius.portfolio.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/skills")
@RequiredArgsConstructor
public class AdminSkillController {

    private final SkillService skillService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse create(@Valid @RequestBody SkillRequest request) {
        return skillService.createSkill(request);
    }

    @PutMapping("/{id}")
    public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest request) {
        return skillService.updateSkill(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skillService.deleteSkill(id);
    }
}
