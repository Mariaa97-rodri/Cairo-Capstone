package com.cairo.cairobackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int  MAX_REQUESTS   = 10;
    private static final long WINDOW_SECONDS = 60L;

    private final ConcurrentHashMap<String, RequestCounter> requestCounts
            = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only applies to auth endpoints — everything else is untouched
        return !request.getServletPath().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String  clientIp = getClientIp(request);
        Instant now      = Instant.now();

        RequestCounter counter = requestCounts.computeIfAbsent(
                clientIp, k -> new RequestCounter(now));

        if (now.isAfter(counter.windowStart.plusSeconds(WINDOW_SECONDS))) {
            counter.count.set(0);
            counter.windowStart = now;
        }

        if (counter.count.incrementAndGet() > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Too many attempts. Please wait 1 minute.\"," +
                            "\"status\":429}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        AtomicInteger count;
        volatile Instant windowStart;

        RequestCounter(Instant windowStart) {
            this.count       = new AtomicInteger(0);
            this.windowStart = windowStart;
        }
    }
}