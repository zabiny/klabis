package com.klabis.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordGenerator Tests")
class PasswordGeneratorTest {

    private PasswordGenerator passwordGenerator;

    @BeforeEach
    void setUp() {
        passwordGenerator = new PasswordGenerator();
    }

    @Nested
    @DisplayName("generate() method - default (no args)")
    class GenerateDefaultMethod {

        @Test
        @DisplayName("should generate password with default length")
        void shouldGenerateDefaultLengthPassword() {
            String password = passwordGenerator.generate();

            assertThat(password).isNotNull();
            assertThat(password).hasSize(16);
        }

        @Test
        @DisplayName("should include uppercase letters")
        void shouldIncludeUppercase() {
            String password = passwordGenerator.generate();

            assertThat(password).matches(".*[A-Z].*");
        }

        @Test
        @DisplayName("should include lowercase letters")
        void shouldIncludeLowercase() {
            String password = passwordGenerator.generate();

            assertThat(password).matches(".*[a-z].*");
        }

        @Test
        @DisplayName("should include digits")
        void shouldIncludeDigits() {
            String password = passwordGenerator.generate();

            assertThat(password).matches(".*\\d.*");
        }

        @Test
        @DisplayName("should include special characters")
        void shouldIncludeSpecialCharacters() {
            String password = passwordGenerator.generate();

            assertThat(password).matches(".*[!@#$%^&*()\\-_+=].*");
        }

        @Test
        @DisplayName("should generate different passwords each time")
        void shouldGenerateDifferentPasswords() {
            String password1 = passwordGenerator.generate();
            String password2 = passwordGenerator.generate();

            assertThat(password1).isNotEqualTo(password2);
        }

        @Test
        @DisplayName("should not contain consecutive duplicate characters")
        void shouldAvoidConsecutiveDuplicates() {
            // Generate multiple passwords and check none have obvious patterns
            for (int i = 0; i < 10; i++) {
                String password = passwordGenerator.generate();
                // Basic check: password should not have 3+ consecutive same chars
                assertThat(password).doesNotMatch("(.)\\1{2,}");
            }
        }
    }

    @Nested
    @DisplayName("generate(int length) method")
    class GenerateWithLengthMethod {

        @Test
        @DisplayName("should generate password with custom length")
        void shouldGenerateCustomLengthPassword() {
            String password = passwordGenerator.generate(20);

            assertThat(password).hasSize(20);
        }

        @Test
        @DisplayName("should reject length less than 12")
        void shouldRejectShortPassword() {
            assertThatThrownBy(() -> passwordGenerator.generate(11))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 12");
        }
    }
}
