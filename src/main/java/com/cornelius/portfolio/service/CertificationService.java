package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationRepository certificationRepository;

    @Transactional(readOnly = true)
    public List<CertificationResponse> getCertifications() {
        return certificationRepository.findAll().stream()
            .map(c -> new CertificationResponse(
                c.getId(), c.getName(), c.getProvider(), c.getIssuedDate(), c.getCredentialUrl()))
            .toList();
    }
}
