package com.eventhub.infrastructure.adapters.in.web;

import com.eventhub.infrastructure.adapters.in.web.dto.ErrorResponse;
import com.eventhub.infrastructure.adapters.in.web.dto.LoginRequest;
import com.eventhub.infrastructure.adapters.in.web.dto.LoginResponse;
import com.eventhub.infrastructure.security.iam.LoginAttemptService;
import com.eventhub.infrastructure.security.iam.PasswordComplexityValidator;
import com.eventhub.infrastructure.security.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final LoginAttemptService loginAttemptService;
    private final PasswordComplexityValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;

    // Hardcoded secure demo user for authentication defense testing
    private static final String DEMO_USER = "admin";
    // BCrypt hash of "Admin@123!"
    private static final String DEMO_PASSWORD_HASH = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"; // or runtime encoded

    public AuthController(LoginAttemptService loginAttemptService,
                          PasswordComplexityValidator passwordValidator,
                          PasswordEncoder passwordEncoder) {
        this.loginAttemptService = loginAttemptService;
        this.passwordValidator = passwordValidator;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = ClientIpResolver.resolveClientIp(httpRequest);
        String username = request.username().trim();

        // Check if account is locked out
        if (loginAttemptService.isLocked(username)) {
            long remainingSeconds = loginAttemptService.getRemainingLockoutSeconds(username);
            String incidentId = UUID.randomUUID().toString();
            log.warn("[AUTH-LOCKED] Attempt to access locked account '{}' from IP: {} (Incident ID: {})",
                    username, clientIp, incidentId);

            return ResponseEntity.status(HttpStatus.LOCKED)
                    .header("Retry-After", String.valueOf(remainingSeconds))
                    .body(ErrorResponse.of(
                            HttpStatus.LOCKED.value(),
                            "Account Locked",
                            "Account is temporarily locked due to multiple failed login attempts. Please retry in " + remainingSeconds + " seconds.",
                            incidentId
                    ));
        }

        // Validate demo credentials (Admin@123!)
        boolean isValid = DEMO_USER.equalsIgnoreCase(username) &&
                ("Admin@123!".equals(request.password()) || passwordEncoder.matches(request.password(), DEMO_PASSWORD_HASH));

        if (!isValid) {
            loginAttemptService.loginFailed(username);
            String incidentId = UUID.randomUUID().toString();
            log.warn("[AUTH-FAILURE] Failed login for user '{}' from IP: {} (Incident ID: {})",
                    username, clientIp, incidentId);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Unauthorized",
                            "Invalid credentials provided.",
                            incidentId
                    ));
        }

        loginAttemptService.loginSucceeded(username);
        log.info("[AUTH-SUCCESS] User '{}' authenticated successfully from IP: {}", username, clientIp);

        String dummyToken = UUID.randomUUID().toString();
        return ResponseEntity.ok(LoginResponse.success(dummyToken, username));
    }

    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        try {
            passwordValidator.validate(password);
            return ResponseEntity.ok(Map.of("valid", true, "message", "Password satisfies complexity requirements"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage()));
        }
    }
}
