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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WafSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("WAF: Should block Path Traversal attack in URI/Query with HTTP 403 Forbidden")
    void shouldBlockPathTraversalInQuery() throws Exception {
        mockMvc.perform(get("/api/events?path=../../../../etc/passwd"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Path Traversal")))
                .andExpect(jsonPath("$.incidentId").isNotEmpty());
    }

    @Test
    @DisplayName("WAF: Should block SQL Injection attack in URI/Query with HTTP 403 Forbidden")
    void shouldBlockSqlInjectionInQuery() throws Exception {
        mockMvc.perform(get("/api/events?sort=UNION+SELECT+1,2,3--"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SQL/NoSQL Injection")))
                .andExpect(jsonPath("$.incidentId").isNotEmpty());
    }

    @Test
    @DisplayName("WAF: Should block Cross-Site Scripting (XSS) attack in Body with HTTP 403 Forbidden")
    void shouldBlockXssInBody() throws Exception {
        String xssPayload = """
                {
                    "eventId": "EVT-XSS-001",
                    "eventType": "PRODUCTION",
                    "description": "<script>alert('xss')</script>",
                    "timestamp": "2026-10-24T20:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(xssPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cross-Site Scripting")))
                .andExpect(jsonPath("$.incidentId").isNotEmpty());
    }

    @Test
    @DisplayName("WAF: Should block Command Injection attack in Body with HTTP 403 Forbidden")
    void shouldBlockCommandInjectionInBody() throws Exception {
        String cmdPayload = """
                {
                    "eventId": "EVT-CMD-001",
                    "eventType": "PRODUCTION",
                    "description": "normal; rm -rf /",
                    "timestamp": "2026-10-24T20:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cmdPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Command Injection")))
                .andExpect(jsonPath("$.incidentId").isNotEmpty());
    }

    @Test
    @DisplayName("WAF: Should allow legitimate clean event creation request")
    void shouldAllowCleanRequest() throws Exception {
        long idSuffix = System.currentTimeMillis();
        String validPayload = """
                {
                    "eventId": "EVT-SAFE-%d",
                    "eventType": "PRODUCTION",
                    "description": "Sensor de temperatura operando normalmente",
                    "timestamp": "2026-10-24T20:00:00"
                }
                """.formatted(idSuffix);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("EVT-SAFE-" + idSuffix))
                .andExpect(jsonPath("$.eventType").value("PRODUCTION"));
    }
}
