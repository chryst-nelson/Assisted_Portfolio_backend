package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.FaqEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findByEnabledTrue();

    List<FaqEntry> findAllByOrderByIdAsc();
}
