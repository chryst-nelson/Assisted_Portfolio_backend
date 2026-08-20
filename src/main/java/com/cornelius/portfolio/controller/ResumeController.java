package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResumeResponse getResume() {
        return resumeService.getActiveResume();
    }
}
