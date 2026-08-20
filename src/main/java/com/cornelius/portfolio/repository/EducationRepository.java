package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {
}
