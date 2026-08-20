package com.cornelius.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "contact_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "other_links", columnDefinition = "jsonb")
    private Map<String, String> otherLinks;
}
