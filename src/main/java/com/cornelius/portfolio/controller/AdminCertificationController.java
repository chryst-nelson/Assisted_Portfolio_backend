package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.CertificationRequest;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/certifications")
@RequiredArgsConstructor
public class AdminCertificationController {

    private final CertificationService certificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CertificationResponse create(@Valid @RequestBody CertificationRequest request) {
        return certificationService.createCertification(request);
    }

    @PutMapping("/{id}")
    public CertificationResponse update(@PathVariable Long id, @Valid @RequestBody CertificationRequest request) {
        return certificationService.updateCertification(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        certificationService.deleteCertification(id);
    }
}
