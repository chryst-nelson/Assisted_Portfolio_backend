package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {

    Optional<ContactInfo> findTopByOrderByIdAsc();
}
