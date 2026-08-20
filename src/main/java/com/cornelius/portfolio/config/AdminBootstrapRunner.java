package com.cornelius.portfolio.config;

import com.cornelius.portfolio.entity.AdminUser;
import com.cornelius.portfolio.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * There's no admin signup flow by design — the first admin account is
 * created from ADMIN_EMAIL / ADMIN_PASSWORD on startup, once, only if the
 * admin_users table is still empty. After that, this is a no-op forever.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-email:}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap-password:}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        if (!StringUtils.hasText(bootstrapEmail) || !StringUtils.hasText(bootstrapPassword)) {
            log.warn("No admin user exists and ADMIN_EMAIL/ADMIN_PASSWORD are not set. "
                + "Set them in .env to bootstrap the first admin account.");
            return;
        }

        AdminUser admin = AdminUser.builder()
            .email(bootstrapEmail)
            .passwordHash(passwordEncoder.encode(bootstrapPassword))
            .role("ADMIN")
            .createdAt(LocalDateTime.now())
            .build();

        adminUserRepository.save(admin);
        log.info("Bootstrapped initial admin user: {}", bootstrapEmail);
    }
}
