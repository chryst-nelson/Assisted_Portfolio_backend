package com.cornelius.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String provider;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "credential_url")
    private String credentialUrl;
}
