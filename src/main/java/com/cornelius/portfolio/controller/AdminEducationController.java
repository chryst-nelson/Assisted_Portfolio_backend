package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.EducationRequest;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.service.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/education")
@RequiredArgsConstructor
public class AdminEducationController {

    private final EducationService educationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EducationResponse create(@Valid @RequestBody EducationRequest request) {
        return educationService.createEducation(request);
    }

    @PutMapping("/{id}")
    public EducationResponse update(@PathVariable Long id, @Valid @RequestBody EducationRequest request) {
        return educationService.updateEducation(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        educationService.deleteEducation(id);
    }
}
