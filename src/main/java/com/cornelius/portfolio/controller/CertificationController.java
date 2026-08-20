package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping
    public List<CertificationResponse> getCertifications() {
        return certificationService.getCertifications();
    }
}
