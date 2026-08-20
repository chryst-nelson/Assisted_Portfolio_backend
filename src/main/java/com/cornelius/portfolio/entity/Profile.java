package com.cornelius.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String interests;

    @Column(name = "career_goals", columnDefinition = "TEXT")
    private String careerGoals;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
