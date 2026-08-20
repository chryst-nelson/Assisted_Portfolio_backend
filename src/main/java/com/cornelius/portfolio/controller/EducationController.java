package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    public List<EducationResponse> getEducation() {
        return educationService.getEducation();
    }
}
