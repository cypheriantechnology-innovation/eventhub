package com.eventhub.security;

import com.eventhub.infrastructure.security.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("File Upload: Should reject malicious path traversal filename")
    void shouldRejectPathTraversalFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../malicious.csv",
                "text/csv",
                "eventId,eventType,description,timestamp\nEVT-1,PRODUCTION,test,2026-10-24T20:00:00Z".getBytes()
        );

        mockMvc.perform(multipart("/api/events/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Malicious path traversal")));
    }

    @Test
    @DisplayName("File Upload: Should reject prohibited file extension (.exe)")
    void shouldRejectForbiddenExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/octet-stream",
                new byte[]{0x4D, 0x5A, 0x00, 0x00}
        );

        mockMvc.perform(multipart("/api/events/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not permitted")));
    }

    @Test
    @DisplayName("File Upload: Should reject disguised Windows PE binary with .csv extension via magic bytes inspection")
    void shouldRejectDisguisedPeBinary() throws Exception {
        byte[] peHeaderWithCsvName = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x00, 0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "stealth_trojan.csv",
                "text/csv",
                peHeaderWithCsvName
        );

        mockMvc.perform(multipart("/api/events/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Executable binary (DOS/PE) detected")));
    }

    @Test
    @DisplayName("File Upload: Should reject disguised Linux ELF binary with .json extension via magic bytes inspection")
    void shouldRejectDisguisedElfBinary() throws Exception {
        byte[] elfHeader = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46, 0x02, 0x01};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rootkit.json",
                "application/json",
                elfHeader
        );

        mockMvc.perform(multipart("/api/events/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Linux executable binary (ELF) detected")));
    }

    @Test
    @DisplayName("File Upload: Should accept legitimate CSV event batch")
    void shouldAcceptValidCsvBatch() throws Exception {
        String csvContent = "eventId,eventType,description,timestamp\nEVT-900,PRODUCTION,Safe event batch,2026-10-24T20:00:00Z";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid_events_october.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/events/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.filename").value("valid_events_october.csv"));
    }
}
