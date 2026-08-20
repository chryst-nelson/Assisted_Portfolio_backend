package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.SkillResponse;
import com.cornelius.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public List<SkillResponse> getSkills(@RequestParam(required = false) String category) {
        return skillService.getSkills(category);
    }
}
