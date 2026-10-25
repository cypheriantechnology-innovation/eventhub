package com.eventhub.unit.security;

import com.eventhub.infrastructure.security.iam.PasswordComplexityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordComplexityValidatorUnitTest {

    private PasswordComplexityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordComplexityValidator();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "short", "Ab1!"})
    @DisplayName("isValid deve retornar false para senhas nulas, vazias ou com menos de 8 caracteres")
    void shouldReturnFalseForShortOrEmptyPasswords(String password) {
        assertThat(validator.isValid(password)).isFalse();
    }

    @Test
    @DisplayName("isValid deve retornar false quando faltar letra maiúscula")
    void shouldReturnFalseWhenMissingUppercase() {
        assertThat(validator.isValid("lowercase123!@#")).isFalse();
    }

    @Test
    @DisplayName("isValid deve retornar false quando faltar letra minúscula")
    void shouldReturnFalseWhenMissingLowercase() {
        assertThat(validator.isValid("UPPERCASE123!@#")).isFalse();
    }

    @Test
    @DisplayName("isValid deve retornar false quando faltar dígito numérico")
    void shouldReturnFalseWhenMissingDigit() {
        assertThat(validator.isValid("NoDigitsPassword!@#")).isFalse();
    }

    @Test
    @DisplayName("isValid deve retornar false quando faltar caractere especial")
    void shouldReturnFalseWhenMissingSpecialChar() {
        assertThat(validator.isValid("NoSpecialCharPassword123")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Admin@123!",
            "Complex#Pass2026",
            "S3cur1ty_EventHub$",
            "Valid!P@ssw0rd"
    })
    @DisplayName("isValid deve retornar true para senhas que atendem a todos os critérios NIST/OWASP")
    void shouldReturnTrueForValidPasswords(String password) {
        assertThat(validator.isValid(password)).isTrue();
        assertThatCode(() -> validator.validate(password)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate deve lançar IllegalArgumentException com mensagem explicativa quando inválido")
    void shouldThrowExceptionWithExplainingMessage() {
        assertThatThrownBy(() -> validator.validate("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");

        assertThatThrownBy(() -> validator.validate("nouppercase123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase");

        assertThatThrownBy(() -> validator.validate("NOLOWERCASE123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");

        assertThatThrownBy(() -> validator.validate("NoDigitsAtAll!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digit");

        assertThatThrownBy(() -> validator.validate("NoSpecialChars123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("special character");
    }
}
