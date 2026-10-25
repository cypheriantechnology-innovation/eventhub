package com.eventhub.security;

import com.eventhub.infrastructure.security.iam.LoginAttemptService;
import com.eventhub.infrastructure.security.iam.PasswordComplexityValidator;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountLockoutSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private PasswordComplexityValidator passwordValidator;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        loginAttemptService.reset();
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Account Defense: Lockout account after 5 consecutive failed attempts with HTTP 423 Locked")
    void shouldLockAccountAfter5FailedAttempts() throws Exception {
        String badCreds = """
                {
                    "username": "victim_user",
                    "password": "WrongPassword!123"
                }
                """;

        // Attempts 1 to 4 should return 401 Unauthorized
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badCreds))
                    .andExpect(status().isUnauthorized());
        }

        // 5th attempt fails and triggers lockout
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCreds))
                .andExpect(status().isUnauthorized());

        // Subsequent attempt should return HTTP 423 Locked with Retry-After header
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCreds))
                .andExpect(status().isLocked())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("temporarily locked")));
    }

    @Test
    @DisplayName("Password Complexity: Validate NIST/OWASP password complexity rules")
    void shouldValidatePasswordComplexity() {
        // Failing tests
        assertFalse(passwordValidator.isValid(null));
        assertFalse(passwordValidator.isValid("short")); // < 8 chars
        assertFalse(passwordValidator.isValid("nouppercase123!")); // No uppercase
        assertFalse(passwordValidator.isValid("NOLOWERCASE123!")); // No lowercase
        assertFalse(passwordValidator.isValid("NoDigitsAtAll!")); // No digits
        assertFalse(passwordValidator.isValid("NoSpecialChars123")); // No special chars

        // Passing tests
        assertTrue(passwordValidator.isValid("SecureP@ss123!"));
        assertTrue(passwordValidator.isValid("Admin@EventHub2026"));
    }
}
