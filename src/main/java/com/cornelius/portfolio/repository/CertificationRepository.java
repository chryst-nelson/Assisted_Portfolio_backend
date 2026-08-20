package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
