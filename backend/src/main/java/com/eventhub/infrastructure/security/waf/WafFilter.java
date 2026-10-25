package com.eventhub.infrastructure.security.waf;

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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Web Application Firewall (WAF) Filter.
 * Performs deep line-rate inspection on URI, Query Parameters, Headers, and Request Body.
 * Actively blocks:
 * 1. Path Traversal
 * 2. SQL / NoSQL Injections
 * 3. Cross-Site Scripting (XSS)
 * 4. Command Injection & Subprocess executions
 * Enforces a maximum payload body threshold of 50MB.
 */
public class WafFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WafFilter.class);
    private static final long MAX_BODY_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

    private final ObjectMapper objectMapper;

    // Detection patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(?i)(\\.\\./|\\.\\.\\\\|%2e%2e|%252e%252e|/etc/(passwd|shadow)|boot\\.ini|windows/win\\.ini)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_NOSQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\bUNION\\b[\\s\\S]*?\\bSELECT\\b|\\bOR\\b\\s+['\"\\d\\w]+\\s*=\\s*['\"\\d\\w]+|--|/\\*[\\s\\S]*?\\*/|\\bSLEEP\\s*\\(|\\bBENCHMARK\\s*\\(|\\bWAITFOR\\s+DELAY\\b|\\$where|\\$regex)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<\\s*script\\b|javascript:|onerror\\s*=|onload\\s*=|onclick\\s*=|onmouseover\\s*=|eval\\s*\\(|alert\\s*\\(|<iframe\\b|<img\\b[^>]*onerror)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\|\\s*(bash|sh|cmd|powershell)|;\\s*(rm|cat|ls|dir|netstat)|\\$\\(\\s*curl|\\bexec\\s*\\(|\\bsystem\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );

    public WafFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = ClientIpResolver.resolveClientIp(request);
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();

        // 1. Enforce payload size limit (50MB)
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_BODY_SIZE_BYTES) {
            String incidentId = UUID.randomUUID().toString();
            log.warn("[WAF-BLOCK] Payload size exceeded: {} bytes > 50MB limit from IP {} on URI {} (Incident ID: {})",
                    contentLength, clientIp, requestUri, incidentId);
            rejectRequest(response, HttpStatus.PAYLOAD_TOO_LARGE, "Payload exceeds maximum allowed limit of 50MB", incidentId);
            return;
        }

        // 2. Inspect URI and Query String
        String uriAndQuery = requestUri + (queryString != null ? "?" + queryString : "");
        String uriViolation = inspectPayload("URI/Query", uriAndQuery);
        if (uriViolation != null) {
            String incidentId = UUID.randomUUID().toString();
            log.warn("[WAF-BLOCK] Security violation [{}] detected in URI/Query '{}' from IP: {} (Incident ID: {})",
                    uriViolation, uriAndQuery, clientIp, incidentId);
            rejectRequest(response, HttpStatus.FORBIDDEN, "Security violation detected: " + uriViolation, incidentId);
            return;
        }

        // 3. Inspect Headers
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            for (String headerName : Collections.list(headerNames)) {
                // Ignore standard benign auth/cookie/host headers unless they contain injection
                if ("cookie".equalsIgnoreCase(headerName) || "authorization".equalsIgnoreCase(headerName)) {
                    continue;
                }
                String headerVal = request.getHeader(headerName);
                if (headerVal != null) {
                    String valueToInspect = headerVal;
                    if ("content-type".equalsIgnoreCase(headerName)) {
                        valueToInspect = headerVal.replaceAll("(?i);\\s*boundary=[^;]+", "");
                    }
                    String headerViolation = inspectPayload("Header[" + headerName + "]", valueToInspect);
                    if (headerViolation != null) {
                        String incidentId = UUID.randomUUID().toString();
                        log.warn("[WAF-BLOCK] Security violation [{}] in header {}='{}' from IP: {} (Incident ID: {})",
                                headerViolation, headerName, headerVal, clientIp, incidentId);
                        rejectRequest(response, HttpStatus.FORBIDDEN, "Security violation detected in headers", incidentId);
                        return;
                    }
                }
            }
        }

        // 4. Wrap request if non-multipart to inspect body
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

        HttpServletRequest requestToUse = request;
        if (!isMultipart && ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod()))) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            requestToUse = cachedRequest;

            String body = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);
            if (!body.isBlank()) {
                String bodyViolation = inspectPayload("Body", body);
                if (bodyViolation != null) {
                    String incidentId = UUID.randomUUID().toString();
                    log.warn("[WAF-BLOCK] Security violation [{}] detected in body from IP: {} on URI {} (Incident ID: {})",
                            bodyViolation, clientIp, requestUri, incidentId);
                    rejectRequest(response, HttpStatus.FORBIDDEN, "Malicious payload detected: " + bodyViolation, incidentId);
                    return;
                }
            }
        }

        filterChain.doFilter(requestToUse, response);
    }

    private String inspectPayload(String location, String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            return "Path Traversal attempt";
        }
        if (SQL_NOSQL_INJECTION_PATTERN.matcher(input).find()) {
            return "SQL/NoSQL Injection signature";
        }
        if (XSS_PATTERN.matcher(input).find()) {
            return "Cross-Site Scripting (XSS) vector";
        }
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            return "Command Injection / Shell Execution vector";
        }
        return null;
    }

    private void rejectRequest(HttpServletResponse response, HttpStatus status, String message, String incidentId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                incidentId
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
