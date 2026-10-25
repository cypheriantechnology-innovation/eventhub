package com.eventhub.unit.security;

import com.eventhub.infrastructure.security.iam.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceUnitTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        loginAttemptService.reset();
    }

    @Test
    @DisplayName("Nova conta não deve estar bloqueada")
    void shouldNotBeLockedInitially() {
        assertThat(loginAttemptService.isLocked("user@example.com")).isFalse();
        assertThat(loginAttemptService.getRemainingLockoutSeconds("user@example.com")).isEqualTo(0);
    }

    @Test
    @DisplayName("Tentativas falhas menores que 5 não devem bloquear a conta")
    void shouldNotLockBefore5Attempts() {
        String username = "testuser";
        for (int i = 1; i <= 4; i++) {
            loginAttemptService.loginFailed(username);
            assertThat(loginAttemptService.isLocked(username)).isFalse();
        }
    }

    @Test
    @DisplayName("5ª tentativa falha consecutiva deve bloquear a conta por 15 minutos (900 segundos)")
    void shouldLockOnFifthFailedAttempt() {
        String username = "targetuser";
        for (int i = 1; i <= 5; i++) {
            loginAttemptService.loginFailed(username);
        }

        assertThat(loginAttemptService.isLocked(username)).isTrue();
        long remaining = loginAttemptService.getRemainingLockoutSeconds(username);
        assertThat(remaining).isBetween(890L, 900L);
    }

    @Test
    @DisplayName("loginSucceeded deve limpar o histórico de falhas e desbloquear o usuário")
    void shouldClearAttemptsOnSuccessfulLogin() {
        String username = "resetuser";
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        loginAttemptService.loginSucceeded(username);

        assertThat(loginAttemptService.isLocked(username)).isFalse();
        // Próxima falha deve recomeçar do contador 1
        for (int i = 1; i <= 4; i++) {
            loginAttemptService.loginFailed(username);
            assertThat(loginAttemptService.isLocked(username)).isFalse();
        }
    }

    @Test
    @DisplayName("Nomes de usuários devem ser case-insensitive (normalização para lowercase)")
    void shouldNormalizeUsernamesToLowerCase() {
        loginAttemptService.loginFailed("JohnDoe");
        loginAttemptService.loginFailed("johndoe");
        loginAttemptService.loginFailed("JOHNDOE");
        loginAttemptService.loginFailed("johnDOE");
        loginAttemptService.loginFailed("jOhNdOe");

        assertThat(loginAttemptService.isLocked("johndoe")).isTrue();
        assertThat(loginAttemptService.isLocked("JohnDoe")).isTrue();
    }
}
