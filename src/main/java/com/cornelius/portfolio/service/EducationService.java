package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.repository.EducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationRepository educationRepository;

    @Transactional(readOnly = true)
    public List<EducationResponse> getEducation() {
        return educationRepository.findAll().stream()
            .map(e -> new EducationResponse(
                e.getId(), e.getInstitution(), e.getDegree(), e.getCourse(),
                e.getDetails(), e.getStartDate(), e.getEndDate()))
            .toList();
    }
}
