package com.cornelius.portfolio.config;

import com.cornelius.portfolio.security.JwtAuthFilter;
import com.cornelius.portfolio.security.RateLimitFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * /api/admin/** requires a valid JWT with ROLE_ADMIN (see JwtAuthFilter).
 * Everything else — including /api/auth/login — is public. Stateless: no
 * sessions, no CSRF token, since every admin request must carry its own
 * bearer token.
 *
 * CORS preflight (OPTIONS) is explicitly permitted before the ROLE_ADMIN
 * rule — browsers send preflight requests anonymously, so without this,
 * Security rejects them before the CORS headers ever get attached.
 *
 * Filter order: RateLimitFilter runs first (M9) — a rate-limited request
 * short-circuits with a 429 before any JWT parsing happens — then
 * JwtAuthFilter, then Spring Security's own chain.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * Both JwtAuthFilter and RateLimitFilter are @Component-annotated Filter
     * beans that are ALSO manually added to Spring Security's chain above.
     * Without disabling their standalone registration, Spring Boot's servlet
     * filter autoconfiguration can register the same bean a second time as
     * an independent servlet filter, running it twice per request. Harmless
     * here (both filters are idempotent) but wasteful and incorrect wiring —
     * this is the standard fix.
     */
    @Bean
    public FilterRegistrationBean<Filter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilterRegistration(RateLimitFilter filter) {
        return disabledRegistration(filter);
    }

    private FilterRegistrationBean<Filter> disabledRegistration(Filter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
