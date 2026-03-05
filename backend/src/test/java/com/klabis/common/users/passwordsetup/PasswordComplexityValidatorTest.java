package com.klabis.common.users.passwordsetup;

import com.klabis.common.users.application.PasswordComplexityValidator;
import com.klabis.common.users.domain.PasswordValidationException;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordComplexityValidator tests")
class PasswordComplexityValidatorTest {

    private final PasswordComplexityValidator validator = new PasswordComplexityValidator();

    @Nested
    @DisplayName("validateBasic() method")
    class ValidateBasicMethod {

        @Test
        @DisplayName("should accept valid complex password")
        void shouldAcceptValidComplexPassword() {
            String validPassword = "SecureP@ssword123";

            validator.validateBasic(validPassword);
            // No exception thrown
        }

        @Test
        @DisplayName("should reject blank password")
        void shouldRejectBlankPassword() {
            assertThatThrownBy(() -> validator.validateBasic("   "))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        @Test
        @DisplayName("should reject password shorter than 12 characters")
        void shouldRejectShortPassword() {
            assertThatThrownBy(() -> validator.validateBasic("Short1!"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 12 characters long");
        }

        @Test
        @DisplayName("should reject password without uppercase letter")
        void shouldRejectPasswordWithoutUppercase() {
            assertThatThrownBy(() -> validator.validateBasic("lowercase123!"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one uppercase letter");
        }

        @Test
        @DisplayName("should reject password without lowercase letter")
        void shouldRejectPasswordWithoutLowercase() {
            assertThatThrownBy(() -> validator.validateBasic("UPPERCASE123!"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one lowercase letter");
        }

        @Test
        @DisplayName("should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            assertThatThrownBy(() -> validator.validateBasic("NoDigitsHere!"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one digit");
        }

        @Test
        @DisplayName("should reject password without special character")
        void shouldRejectPasswordWithoutSpecialCharacter() {
            assertThatThrownBy(() -> validator.validateBasic("NoSpecialChars123"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one special character");
        }

        @Test
        @DisplayName("should reject password with multiple complexity issues")
        void shouldRejectPasswordWithMultipleComplexityIssues() {
            assertThatThrownBy(() -> validator.validateBasic("simple"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 12 characters long")
                    .hasMessageContaining("Password must contain at least one uppercase letter")
                    .hasMessageContaining("Password must contain at least one digit")
                    .hasMessageContaining("Password must contain at least one special character");
        }

        @Test
        @DisplayName("should accept password with all special characters")
        void shouldAcceptPasswordWithAllSpecialCharacters() {
            String password = "Test!@#$%^&*()123"; // Contains multiple special characters

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with spaces")
        void shouldAcceptPasswordWithSpaces() {
            String password = "My Secure Pass 123!"; // Contains spaces

            validator.validateBasic(password);
            // No exception thrown
        }

        // Boundary condition tests

        @Test
        @DisplayName("should reject password exactly 11 characters (one below minimum)")
        void shouldRejectPasswordExactly11Characters() {
            String password = "Short11!Ch"; // Exactly 11 characters

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 12 characters long");
        }

        @Test
        @DisplayName("should accept password exactly 12 characters (boundary)")
        void shouldAcceptPasswordExactly12CharactersBoundary() {
            String password = "Valid12!Chab"; // Exactly 12 characters with all requirements

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password exactly 13 characters")
        void shouldAcceptPasswordExactly13Characters() {
            String password = "Valid12!Chabc"; // Exactly 13 characters with all requirements

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept very long password")
        void shouldAcceptVeryLongPassword() {
            String password = "VeryLongPassword123!@#WithLotsOfChars";

            validator.validateBasic(password);
            // No exception thrown
        }

        // Special character tests - test each special character from the regex pattern

        @Test
        @DisplayName("should accept password with exclamation mark")
        void shouldAcceptPasswordWithExclamationMark() {
            String password = "ValidPassword123!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with at sign")
        void shouldAcceptPasswordWithAtSign() {
            String password = "ValidPassword123@";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with hash")
        void shouldAcceptPasswordWithHash() {
            String password = "ValidPassword123#";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with dollar sign")
        void shouldAcceptPasswordWithDollarSign() {
            String password = "ValidPassword123$";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with percent")
        void shouldAcceptPasswordWithPercent() {
            String password = "ValidPassword123%";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with caret")
        void shouldAcceptPasswordWithCaret() {
            String password = "ValidPassword123^";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with ampersand")
        void shouldAcceptPasswordWithAmpersand() {
            String password = "ValidPassword123&";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with asterisk")
        void shouldAcceptPasswordWithAsterisk() {
            String password = "ValidPassword123*";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with parenthesis")
        void shouldAcceptPasswordWithParenthesis() {
            String password = "ValidPassword123()";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with underscore")
        void shouldAcceptPasswordWithUnderscore() {
            String password = "ValidPassword123_";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with plus")
        void shouldAcceptPasswordWithPlus() {
            String password = "ValidPassword123+";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with minus")
        void shouldAcceptPasswordWithMinus() {
            String password = "ValidPassword123-";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with equals")
        void shouldAcceptPasswordWithEquals() {
            String password = "ValidPassword123=";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with square brackets")
        void shouldAcceptPasswordWithSquareBrackets() {
            String password = "ValidPassword123[]";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with curly braces")
        void shouldAcceptPasswordWithCurlyBraces() {
            String password = "ValidPassword123{}";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with semicolon")
        void shouldAcceptPasswordWithSemicolon() {
            String password = "ValidPassword123;";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with colon")
        void shouldAcceptPasswordWithColon() {
            String password = "ValidPassword123:";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with single quote")
        void shouldAcceptPasswordWithSingleQuote() {
            String password = "ValidPassword123'";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with double quote")
        void shouldAcceptPasswordWithDoubleQuote() {
            String password = "ValidPassword123\"";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with pipe")
        void shouldAcceptPasswordWithPipe() {
            String password = "ValidPassword123|";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with comma")
        void shouldAcceptPasswordWithComma() {
            String password = "ValidPassword123,";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with period")
        void shouldAcceptPasswordWithPeriod() {
            String password = "ValidPassword123.";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with less than greater than")
        void shouldAcceptPasswordWithLessThanGreaterThan() {
            String password = "ValidPassword123<>";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with question mark")
        void shouldAcceptPasswordWithQuestionMark() {
            String password = "ValidPassword123?";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with slash")
        void shouldAcceptPasswordWithSlash() {
            String password = "ValidPassword123/";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with backslash")
        void shouldAcceptPasswordWithBackslash() {
            String password = "ValidPassword123\\";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with backtick")
        void shouldAcceptPasswordWithBacktick() {
            String password = "ValidPassword123`";

            validator.validateBasic(password);
            // No exception thrown
        }

        // Casing tests

        @Test
        @DisplayName("should reject password with all uppercase (no lowercase)")
        void shouldRejectPasswordWithAllUppercase() {
            String password = "ALLUPPERCASE123!";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one lowercase letter");
        }

        @Test
        @DisplayName("should reject password with all lowercase (no uppercase)")
        void shouldRejectPasswordWithAllLowercase() {
            String password = "alllowercase123!";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one uppercase letter");
        }

        @Test
        @DisplayName("should accept password with mixed casing")
        void shouldAcceptPasswordWithMixedCasing() {
            String password = "MiXeCaSe123!@#";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with single uppercase letter")
        void shouldAcceptPasswordWithSingleUppercaseLetter() {
            String password = "aaaaaaa123!A"; // Single uppercase at the end (13 chars)

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with single lowercase letter")
        void shouldAcceptPasswordWithSingleLowercaseLetter() {
            String password = "AAAAAAA123!a"; // Single lowercase at the end

            validator.validateBasic(password);
            // No exception thrown
        }

        // Digit tests

        @Test
        @DisplayName("should accept password with single digit")
        void shouldAcceptPasswordWithSingleDigit() {
            String password = "ValidPassword!!1";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with multiple digits")
        void shouldAcceptPasswordWithMultipleDigits() {
            String password = "ValidPassword123456789!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with only digits (but meets other requirements)")
        void shouldAcceptPasswordWithDigitsAndSpecialAndMixedCase() {
            String password = "ABCdef123!@#456";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should reject password with no digits")
        void shouldRejectPasswordWithNoDigits() {
            String password = "NoDigitsHere!@#";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one digit");
        }

        // Whitespace tests

        @Test
        @DisplayName("should accept password with leading whitespace")
        void shouldAcceptPasswordWithLeadingWhitespace() {
            String password = "  ValidPassword123!"; // Leading spaces

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with trailing whitespace")
        void shouldAcceptPasswordWithTrailingWhitespace() {
            String password = "ValidPassword123!  "; // Trailing spaces

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with multiple spaces")
        void shouldAcceptPasswordWithMultipleSpaces() {
            String password = "Valid   Password   123!"; // Multiple spaces

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with tabs")
        void shouldAcceptPasswordWithTabs() {
            String password = "Valid\tPassword\t123!"; // Contains tabs

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with newlines")
        void shouldAcceptPasswordWithNewlines() {
            String password = "Valid\nPassword\n123!"; // Contains newlines

            validator.validateBasic(password);
            // No exception thrown
        }

        // Null and empty password tests

        @Test
        @DisplayName("should reject null password")
        void shouldRejectNullPasswordBasic() {
            assertThatThrownBy(() -> validator.validateBasic(null))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        @Test
        @DisplayName("should reject empty password")
        void shouldRejectEmptyPassword() {
            String password = "";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        @Test
        @DisplayName("should reject password with only spaces")
        void shouldRejectPasswordWithOnlySpaces() {
            String password = "     ";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        @Test
        @DisplayName("should reject password with only tabs")
        void shouldRejectPasswordWithOnlyTabs() {
            String password = "\t\t\t";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        @Test
        @DisplayName("should reject password with mixed whitespace only")
        void shouldRejectPasswordWithMixedWhitespaceOnly() {
            String password = " \t \n \r ";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessage("Password is required");
        }

        // Unicode character tests

        @Test
        @DisplayName("should accept password with accented characters")
        void shouldAcceptPasswordWithAccentedCharacters() {
            String password = "ValidPässwörd123!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with Cyrillic characters")
        void shouldAcceptPasswordWithCyrillicCharacters() {
            String password = "ValidПароль123!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with Greek characters")
        void shouldAcceptPasswordWithGreekCharacters() {
            String password = "ValidΚωδικός123!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with Chinese characters")
        void shouldAcceptPasswordWithChineseCharacters() {
            String password = "Valid密码密码123!"; // Longer to meet 12 char requirement

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with Japanese characters")
        void shouldAcceptPasswordWithJapaneseCharacters() {
            String password = "Validパスワード123!";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with emoji")
        void shouldAcceptPasswordWithEmoji() {
            String password = "ValidPassword123!😀";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password with mathematical symbols")
        void shouldAcceptPasswordWithMathematicalSymbols() {
            String password = "ValidPassword123!±≠≤≥";

            validator.validateBasic(password);
            // No exception thrown
        }

        // Edge case: password with all special characters but no letters

        @Test
        @DisplayName("should reject password with only special characters and digits")
        void shouldRejectPasswordWithOnlySpecialCharsAndDigits() {
            String password = "1234567890!@#$%";

            assertThatThrownBy(() -> validator.validateBasic(password))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must contain at least one uppercase letter")
                    .hasMessageContaining("Password must contain at least one lowercase letter");
        }

        // Edge case: password at minimum length with each requirement minimally met

        @Test
        @DisplayName("should accept password at minimum length with minimal requirements")
        void shouldAcceptPasswordAtMinimumLengthWithMinimalRequirements() {
            // 12 chars: 1 upper, 1 lower, 1 digit, 1 special, 8 more chars
            String password = "Aa1!bbbbbbbb";

            validator.validateBasic(password);
            // No exception thrown
        }

        @Test
        @DisplayName("should accept password where special character is at different positions")
        void shouldAcceptPasswordWhereSpecialCharacterIsAtDifferentPositions() {
            // Special at start
            validator.validateBasic("!ValidPass123");
            // Special in middle
            validator.validateBasic("Valid!Pass123");
            // Special at end
            validator.validateBasic("ValidPass123!");

            // All should pass without exception
        }

        @Test
        @DisplayName("should accept password where digit is at different positions")
        void shouldAcceptPasswordWhereDigitIsAtDifferentPositions() {
            // Digit at start
            validator.validateBasic("1ValidPass!@#");
            // Digit in middle
            validator.validateBasic("Valid1Pass!@#");
            // Digit at end
            validator.validateBasic("ValidPass!@#1");

            // All should pass without exception
        }

        @Test
        @DisplayName("should accept password with repeated characters")
        void shouldAcceptPasswordWithRepeatedCharacters() {
            String password = "AAAAAA111111!!!!!!aa"; // Added lowercase to meet requirement

            validator.validateBasic(password);
            // No exception thrown
        }
    }

    @Nested
    @DisplayName("validate() method with member context")
    class ValidateMethodWithMemberContext {

        @Test
        @DisplayName("should accept valid password with member context")
        void shouldAcceptValidPasswordWithMemberContext() {
            Member member = createTestMember("John", "Doe", "ZBM0101");
            String validPassword = "SecureP@ssword123";

            validator.validate(validPassword,
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue());
            // No exception thrown
        }

        @Test
        @DisplayName("should reject password containing first name")
        void shouldRejectPasswordContainingFirstName() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            assertThatThrownBy(() -> validator.validate("john123!@#ABC",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password cannot contain your first name");
        }

        @Test
        @DisplayName("should reject password containing last name")
        void shouldRejectPasswordContainingLastName() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            assertThatThrownBy(() -> validator.validate("doe123!@#ABC",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password cannot contain your last name");
        }

        @Test
        @DisplayName("should reject password containing registration number")
        void shouldRejectPasswordContainingRegistrationNumber() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            assertThatThrownBy(() -> validator.validate("ZBM0101abc!@#XYZ",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password cannot contain your registration number");
        }

        @Test
        @DisplayName("should reject password containing first name case insensitive")
        void shouldRejectPasswordContainingFirstNameCaseInsensitive() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            assertThatThrownBy(() -> validator.validate("JOHNabc123!@#",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password cannot contain your first name");

            assertThatThrownBy(() -> validator.validate("Johnabc123!@#",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password cannot contain your first name");
        }

        @Test
        @DisplayName("should reject password containing multiple personal info fields")
        void shouldRejectPasswordContainingMultiplePersonalInfoFields() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            assertThatThrownBy(() -> validator.validate("john doe ZBM0101!",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("first name")
                    .hasMessageContaining("last name")
                    .hasMessageContaining("registration number");
        }

        @Test
        @DisplayName("should apply both basic and personal info validation")
        void shouldApplyBothBasicAndPersonalInfoValidation() {
            Member member = createTestMember("John", "Doe", "ZBM0101");

            // Password fails both basic validation and contains personal info
            assertThatThrownBy(() -> validator.validate("john",
                    member.getFirstName(),
                    member.getLastName(),
                    member.getRegistrationNumber().getValue()))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 12 characters long");
        }

        // Helper methods

        private Member createTestMember(String firstName, String lastName, String registrationNumber) {
            return aMember()
                    .withRegistrationNumber(registrationNumber)
                    .withName(firstName, lastName)
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withNationality("IT")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Via Roma 123", "Milano", "20100", "IT"))
                    .withEmail("test@example.com")
                    .withPhone("+420777888999")
                    .withNoGuardian()
                    .build();
        }
    }
}
