package com.eventhub.security;

import com.eventhub.infrastructure.security.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Rate Limiter: Should enforce Tier 1 limit (8 req/min) on /api/auth and return HTTP 429 on 9th request")
    void shouldEnforceTier1AuthRateLimiting() throws Exception {
        // First 8 requests from this IP should be allowed through rate limiter
        for (int i = 0; i < 8; i++) {
            String payload = """
                    {
                        "username": "user_%d",
                        "password": "WrongPassword!123"
                    }
                    """.formatted(i);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
        }

        // 9th request must be intercepted by RateLimitingFilter with HTTP 429
        String finalPayload = """
                {
                    "username": "user_overflow",
                    "password": "WrongPassword!123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Rate limit exceeded for AUTH tier")));
    }
}
