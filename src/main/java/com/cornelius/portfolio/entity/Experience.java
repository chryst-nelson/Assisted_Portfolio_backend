package com.cornelius.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "experience_technologies",
        joinColumns = @JoinColumn(name = "experience_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> technologies = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "experience", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExperienceAchievement> achievements = new ArrayList<>();
}
