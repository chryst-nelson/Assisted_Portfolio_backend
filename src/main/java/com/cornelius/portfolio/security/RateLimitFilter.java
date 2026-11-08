package com.cornelius.portfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory, per-IP fixed-window rate limiter. Deliberately not a
 * distributed store (Redis etc.) — this app runs as a single instance, and
 * adding that infrastructure for a personal portfolio site's traffic volume
 * would be complexity with no real benefit.
 *
 * Two targets, chosen for where the actual risk is:
 *  - /api/chat and /api/chat/stream: each request runs the answer engine (cheap, but still
 *    CPU and DB work on a small EC2 box); both are public.
 *  - /api/auth/login: brute-force protection on the one admin account.
 *
 * Fixed-window rather than sliding-window/token-bucket: has a known edge
 * case (a burst right at a window boundary can briefly allow close to 2x
 * the limit), but that's an acceptable tradeoff here — the threat model is
 * "stop a single bad actor from loading the server or brute-forcing a
 * login," not "defend against a coordinated distributed attack." A fixed
 * window is simple enough to verify correct by reading it.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CHAT_LIMIT = 30;
    private static final long CHAT_WINDOW_MS = 60_000;

    private static final int LOGIN_LIMIT = 5;
    private static final long LOGIN_WINDOW_MS = 60_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Limit limit = resolveLimit(request.getRequestURI());

        if (limit != null) {
            String key = limit.name() + ":" + clientIp(request);
            if (!tryConsume(key, limit.maxRequests(), limit.windowMillis())) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                // Matches ApiError's shape (timestamp/status/error/message/path)
                // rather than a one-off ad-hoc format — every other error
                // response in this app has this same structure, and the
                // frontend's error parsing expects it consistently.
                response.getWriter().write(
                    "{\"timestamp\":\"" + Instant.now() + "\","
                        + "\"status\":429,"
                        + "\"error\":\"Too Many Requests\","
                        + "\"message\":\"Rate limit exceeded. Please wait before trying again.\","
                        + "\"path\":\"" + request.getRequestURI() + "\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(String path) {
        if (path.startsWith("/api/chat")) {
            return new Limit("chat", CHAT_LIMIT, CHAT_WINDOW_MS);
        }
        if (path.equals("/api/auth/login")) {
            return new Limit("login", LOGIN_LIMIT, LOGIN_WINDOW_MS);
        }
        return null;
    }

    private boolean tryConsume(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));

        synchronized (window) {
            if (now - window.windowStart >= windowMillis) {
                window.windowStart = now;
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= maxRequests;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Not urgent at this project's realistic traffic volume — a personal
    // portfolio site would take an unrealistic number of unique visitor IPs
    // to accumulate meaningful memory here. Included anyway: it's a few
    // lines, and it's better hygiene than explaining later why it's absent.
    @Scheduled(fixedRate = 600_000) // every 10 minutes
    void cleanupStaleWindows() {
        long cutoff = System.currentTimeMillis() - 3_600_000; // 1 hour of inactivity
        windows.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }

    private record Limit(String name, int maxRequests, long windowMillis) {}

    private static final class Window {
        private volatile long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
