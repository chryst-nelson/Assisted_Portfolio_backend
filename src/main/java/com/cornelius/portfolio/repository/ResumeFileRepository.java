package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.ResumeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeFileRepository extends JpaRepository<ResumeFile, Long> {

    Optional<ResumeFile> findByActiveTrue();
}
