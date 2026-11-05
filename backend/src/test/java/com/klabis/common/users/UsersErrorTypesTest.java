package com.klabis.common.users;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.MemberProfileRequiredException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.ratelimit.RateLimitExceededException;
import com.klabis.common.users.application.PasswordComplexityValidator;
import com.klabis.common.users.domain.AuthorityValidator;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.CannotRemoveLastPermissionManagerException;
import com.klabis.common.users.domain.IncorrectCurrentPasswordException;
import com.klabis.common.users.domain.InvalidAuthorityException;
import com.klabis.common.users.domain.PasswordValidationException;
import com.klabis.common.users.domain.PasswordValidationException.Violation;
import com.klabis.common.users.domain.PasswordsDoNotMatchException;
import com.klabis.common.users.domain.TokenAlreadyUsedException;
import com.klabis.common.users.domain.TokenExpiredException;
import com.klabis.common.users.domain.TokenValidationException;
import com.klabis.common.users.domain.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Common/users error types")
class UsersErrorTypesTest {

    @Test
    @DisplayName("an unknown user is a 404 without parameters")
    void userNotFound() {
        UserNotFoundException ex = new UserNotFoundException("User not found");

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class).isInstanceOf(UserFacingError.class);
        assertThat(ex.errorType()).isEqualTo("user-not-found");
        assertThat(ex.messageParameters()).isEmpty();
    }

    @Test
    @DisplayName("the permission lockouts are conflicts of their own types")
    void permissionLockouts() {
        CannotRemoveLastPermissionManagerException lastManager = new CannotRemoveLastPermissionManagerException();
        AuthorizationPolicy.AdminLockoutException lockout = new AuthorizationPolicy.AdminLockoutException("lockout");

        assertThat(lastManager).isInstanceOf(ConflictException.class);
        assertThat(lastManager.errorType()).isEqualTo("cannot-remove-last-permission-manager");
        assertThat(lockout).isInstanceOf(ConflictException.class);
        assertThat(lockout.errorType()).isEqualTo("admin-lockout");
        assertThat(lastManager.messageParameters()).isEmpty();
        assertThat(lockout.messageParameters()).isEmpty();
    }

    @Test
    @DisplayName("an unknown authority value is reported by the value")
    void invalidAuthority() {
        assertThatThrownBy(() -> AuthorityValidator.validate(Set.of("NOPE:NOPE")))
                .isInstanceOfSatisfying(InvalidAuthorityException.class, ex -> {
                    assertThat(ex).isInstanceOf(InvalidDataException.class);
                    assertThat(ex.errorType()).isEqualTo("invalid-authority");
                    assertThat(ex.messageParameters()).containsOnly(Map.entry("authority", "NOPE:NOPE"));
                });
    }

    @Test
    @DisplayName("a wrong current password is bound to the current password field")
    void incorrectCurrentPassword() throws Exception {
        IncorrectCurrentPasswordException ex = new IncorrectCurrentPasswordException();

        assertThat(ex.errorType()).isEqualTo("incorrect-current-password");
        assertThat(ex.field()).isEqualTo("currentPassword");
        assertThat(ex.messageParameters()).isEmpty();
        assertThat(recordComponents("com.klabis.common.users.infrastructure.restapi.ChangePasswordRequest"))
                .contains("currentPassword");
    }

    @Test
    @DisplayName("mismatching passwords are bound to the confirmation field")
    void passwordsDoNotMatch() throws Exception {
        PasswordsDoNotMatchException ex = new PasswordsDoNotMatchException();

        assertThat(ex).isInstanceOf(InvalidDataException.class);
        assertThat(ex.errorType()).isEqualTo("passwords-do-not-match");
        assertThat(ex.field()).isEqualTo("passwordConfirmation");
        assertThat(recordComponents("com.klabis.common.users.infrastructure.restapi.SetPasswordRequest"))
                .contains("passwordConfirmation");
    }

    @Test
    @DisplayName("a weak password reports every violated rule and the minimum length")
    void weakPassword() {
        assertThatThrownBy(() -> new PasswordComplexityValidator().validateBasic("abc"))
                .isInstanceOfSatisfying(PasswordValidationException.class, ex -> {
                    assertThat(ex.errorType()).isEqualTo("password-requirements-not-met");
                    assertThat(ex.messageParameters()).containsOnlyKeys("violations", "minLength");
                    assertThat(ex.messageParameters().get("minLength")).isEqualTo(12);
                    assertThat((List<String>) ex.messageParameters().get("violations")).containsExactly(
                            "TOO_SHORT", "MISSING_UPPERCASE", "MISSING_DIGIT", "MISSING_SPECIAL_CHARACTER");
                    assertThat(ex.getMessage()).contains("Password must be at least 12 characters long");
                });
    }

    @Test
    @DisplayName("a blank password is reported as required")
    void blankPassword() {
        assertThatThrownBy(() -> new PasswordComplexityValidator().validateBasic(" "))
                .isInstanceOfSatisfying(PasswordValidationException.class, ex ->
                        assertThat((List<String>) ex.messageParameters().get("violations")).containsExactly("REQUIRED"));
    }

    @Test
    @DisplayName("a password containing personal information reports which one, never the value")
    void personalInformation() {
        assertThatThrownBy(() -> new PasswordComplexityValidator()
                .validate("Novakovic-Jan-2024!", "Jan", "Novakovic", "ZBM9000"))
                .isInstanceOfSatisfying(PasswordValidationException.class, ex -> {
                    assertThat((List<String>) ex.messageParameters().get("violations"))
                            .containsExactly("CONTAINS_FIRST_NAME", "CONTAINS_LAST_NAME");
                    assertThat(ex.messageParameters().toString()).doesNotContain("Novakovic");
                });
        assertThat(Violation.values()).hasSize(9);
    }

    @Test
    @DisplayName("token errors have one type per kind and the base type for everything else")
    void tokenErrors() {
        assertThat(new TokenValidationException("User not found").errorType()).isEqualTo("invalid-token");
        assertThat(new TokenExpiredException("Token has expired").errorType()).isEqualTo("token-expired");
        assertThat(new TokenAlreadyUsedException("Token has already been used").errorType()).isEqualTo("token-already-used");
        assertThat(new TokenValidationException("x").messageParameters()).isEmpty();
    }

    @Test
    @DisplayName("authorization errors have their own types without parameters")
    void authorizationErrors() {
        InsufficientAuthorityException insufficient = new InsufficientAuthorityException("GROUPS:TRAINING");
        MemberProfileRequiredException profile = new MemberProfileRequiredException();

        assertThat(insufficient.errorType()).isEqualTo("insufficient-authority");
        assertThat(insufficient.messageParameters()).isEmpty();
        assertThat(profile.errorType()).isEqualTo("member-profile-required");
        assertThat(profile.messageParameters()).isEmpty();
    }

    @Test
    @DisplayName("the rate limit has its own type")
    void rateLimit() {
        assertThat(new RateLimitExceededException("limit").errorType()).isEqualTo("rate-limit-exceeded");
    }

    private static Set<String> recordComponents(String className) throws ClassNotFoundException {
        return Set.copyOf(Arrays.stream(Class.forName(className).getRecordComponents())
                .map(RecordComponent::getName).toList());
    }
}
