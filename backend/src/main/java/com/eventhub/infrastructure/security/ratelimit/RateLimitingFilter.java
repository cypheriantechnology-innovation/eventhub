package com.eventhub.infrastructure.security.ratelimit;

import com.eventhub.infrastructure.adapters.in.web.dto.ErrorResponse;
import com.eventhub.infrastructure.security.util.ClientIpResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Multi-Tier Rate Limiting Filter.
 * Enforces sliding window thresholds per client IP:
 * - Tier 1: Auth endpoints (/api/auth/**) -> 8 req / 60s
 * - Tier 2: Mutations & Uploads (POST/PUT/DELETE /api/events/**, /api/events/upload) -> 40 req / 60s
 * - Tier 3: General API (/api/**) -> 400 req / 60s
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final long WINDOW_MS = 60_000L; // 60 seconds
    private static final int TIER_1_AUTH_MAX = 8;
    private static final int TIER_2_MUTATION_MAX = 40;
    private static final int TIER_3_GENERAL_MAX = 400;

    private final SlidingWindowRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(SlidingWindowRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith("/api/") || "/api/events/stream".equals(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIpResolver.resolveClientIp(request);
        String method = request.getMethod();

        String tier;
        int maxRequests;

        if (requestUri.startsWith("/api/auth")) {
            tier = "AUTH";
            maxRequests = TIER_1_AUTH_MAX;
        } else if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))
                || requestUri.startsWith("/api/events/upload")) {
            tier = "MUTATION";
            maxRequests = TIER_2_MUTATION_MAX;
        } else {
            tier = "GENERAL";
            maxRequests = TIER_3_GENERAL_MAX;
        }

        String rateLimitKey = clientIp + ":" + tier;
        SlidingWindowRateLimiter.RateLimitResult result = rateLimiter.tryAcquire(rateLimitKey, maxRequests, WINDOW_MS);

        if (!result.isAllowed()) {
            String incidentId = UUID.randomUUID().toString();
            log.warn("[RATE-LIMIT-EXCEEDED] Client IP: {} exceeded limit for Tier: {} ({} req/60s). Retry after: {}s (Incident ID: {})",
                    clientIp, tier, maxRequests, result.retryAfterSeconds(), incidentId);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorResponse errorResponse = ErrorResponse.of(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Rate limit exceeded for " + tier + " tier. Maximum allowed is " + maxRequests + " requests per minute.",
                    incidentId
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
