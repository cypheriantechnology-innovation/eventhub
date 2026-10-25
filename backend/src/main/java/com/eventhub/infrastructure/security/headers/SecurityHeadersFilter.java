package com.eventhub.infrastructure.security.headers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Hardens HTTP Responses with modern OWASP Top 10 Security Headers.
 * Suppresses fingerprinting headers and guarantees defense against clickjacking,
 * MIME sniffing, unencrypted transport, and sensitive API caching.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP_POLICY =
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self'; " +
            "object-src 'none'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'";

    private static final String PERMISSIONS_POLICY =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), vr=()";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Content Security Policy (CSP)
        response.setHeader("Content-Security-Policy", CSP_POLICY);

        // 2. Clickjacking Protection
        response.setHeader("X-Frame-Options", "DENY");

        // 3. MIME Sniffing Protection
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 4. HTTP Strict Transport Security (HSTS)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // 5. Cross-Site Scripting Protection (Legacy browser support)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 6. Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 7. Permissions Policy (Feature Policy)
        response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);

        // 8. Obfuscate Server Signature & prevent information disclosure
        response.setHeader("Server", "EventHub-Gateway");

        // 9. Sensitive API Caching Prevention
        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}
