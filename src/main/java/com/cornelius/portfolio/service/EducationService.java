package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.EducationRequest;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.entity.Education;
import com.cornelius.portfolio.exception.NotFoundException;
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
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EducationResponse createEducation(EducationRequest request) {
        Education education = Education.builder()
            .institution(request.institution())
            .degree(request.degree())
            .course(request.course())
            .details(request.details())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .build();
        return toResponse(educationRepository.save(education));
    }

    @Transactional
    public EducationResponse updateEducation(Long id, EducationRequest request) {
        Education education = educationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No education entry found with id " + id));

        education.setInstitution(request.institution());
        education.setDegree(request.degree());
        education.setCourse(request.course());
        education.setDetails(request.details());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());

        return toResponse(educationRepository.save(education));
    }

    @Transactional
    public void deleteEducation(Long id) {
        if (!educationRepository.existsById(id)) {
            throw new NotFoundException("No education entry found with id " + id);
        }
        educationRepository.deleteById(id);
    }

    private EducationResponse toResponse(Education e) {
        return new EducationResponse(
            e.getId(), e.getInstitution(), e.getDegree(), e.getCourse(),
            e.getDetails(), e.getStartDate(), e.getEndDate());
    }
}
