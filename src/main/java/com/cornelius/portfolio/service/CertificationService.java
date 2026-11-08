package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.CertificationRequest;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.entity.Certification;
import com.cornelius.portfolio.exception.NotFoundException;
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
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CertificationResponse createCertification(CertificationRequest request) {
        Certification certification = Certification.builder()
            .name(request.name())
            .provider(request.provider())
            .issuedDate(request.issuedDate())
            .credentialUrl(request.credentialUrl())
            .build();
        return toResponse(certificationRepository.save(certification));
    }

    @Transactional
    public CertificationResponse updateCertification(Long id, CertificationRequest request) {
        Certification certification = certificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No certification found with id " + id));

        certification.setName(request.name());
        certification.setProvider(request.provider());
        certification.setIssuedDate(request.issuedDate());
        certification.setCredentialUrl(request.credentialUrl());

        return toResponse(certificationRepository.save(certification));
    }

    @Transactional
    public void deleteCertification(Long id) {
        if (!certificationRepository.existsById(id)) {
            throw new NotFoundException("No certification found with id " + id);
        }
        certificationRepository.deleteById(id);
    }

    private CertificationResponse toResponse(Certification c) {
        return new CertificationResponse(c.getId(), c.getName(), c.getProvider(), c.getIssuedDate(), c.getCredentialUrl());
    }
}
