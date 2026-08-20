package com.cornelius.portfolio.repository;

import com.cornelius.portfolio.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySlug(String slug);

    @Query("""
        SELECT DISTINCT p FROM Project p
        JOIN p.technologies t
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :tech, '%'))
        """)
    List<Project> searchByTechnology(@Param("tech") String tech);
}
