package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.ResumeUploadRequest;
import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.entity.ResumeFile;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.repository.ResumeFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeFileRepository resumeFileRepository;

    @Transactional(readOnly = true)
    public ResumeResponse getActiveResume() {
        ResumeFile resume = resumeFileRepository.findByActiveTrue()
            .orElseThrow(() -> new NotFoundException("No active resume is available yet."));
        return toResponse(resume);
    }

    // Deactivates whatever resume is currently active, then stores the new one as active.
    // Takes an already-hosted file URL rather than a raw upload — see README for why.
    @Transactional
    public ResumeResponse uploadResume(ResumeUploadRequest request) {
        resumeFileRepository.findByActiveTrue().ifPresent(existing -> {
            existing.setActive(false);
            resumeFileRepository.save(existing);
        });

        ResumeFile resume = ResumeFile.builder()
            .fileUrl(request.fileUrl())
            .version(request.version())
            .uploadedAt(LocalDateTime.now())
            .active(true)
            .build();

        return toResponse(resumeFileRepository.save(resume));
    }

    private ResumeResponse toResponse(ResumeFile resume) {
        return new ResumeResponse(resume.getFileUrl(), resume.getVersion(), resume.getUploadedAt());
    }
}
