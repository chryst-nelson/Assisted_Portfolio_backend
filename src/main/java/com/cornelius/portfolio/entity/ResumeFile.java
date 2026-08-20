package com.cornelius.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    private String version;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
