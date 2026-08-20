package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.ResumeUploadRequest;
import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resume")
@RequiredArgsConstructor
public class AdminResumeController {

    private final ResumeService resumeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse upload(@Valid @RequestBody ResumeUploadRequest request) {
        return resumeService.uploadResume(request);
    }
}
