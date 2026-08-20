package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.LoginRequest;
import com.cornelius.portfolio.dto.response.LoginResponse;
import com.cornelius.portfolio.entity.AdminUser;
import com.cornelius.portfolio.exception.UnauthorizedException;
import com.cornelius.portfolio.repository.AdminUserRepository;
import com.cornelius.portfolio.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.email())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = jwtService.generateToken(admin.getEmail(), admin.getRole());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}
