package com.klabis.common.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthorityValidator tests")
class AuthorityValidatorTest {

    @Nested
    @DisplayName("validate(Set<String>) method")
    class ValidateStringSetMethod {

        @Test
        @DisplayName("should accept valid authority strings")
        void shouldAcceptValidAuthorityStrings() {
            // No exception should be thrown
            AuthorityValidator.validate(Set.of("MEMBERS:READ", "MEMBERS:CREATE"));
        }

        @Test
        @DisplayName("should accept single valid authority")
        void shouldAcceptSingleValidAuthority() {
            AuthorityValidator.validate(Set.of("MEMBERS:READ"));
        }

        @Test
        @DisplayName("should accept all valid authorities")
        void shouldAcceptAllValidAuthorities() {
            Set<String> allAuthorities = Set.of(
                    "CALENDAR:MANAGE",
                    "MEMBERS:CREATE",
                    "MEMBERS:READ",
                    "MEMBERS:UPDATE",
                    "MEMBERS:DELETE",
                    "MEMBERS:PERMISSIONS",
                    "EVENTS:READ",
                    "EVENTS:MANAGE"
            );
            AuthorityValidator.validate(allAuthorities);
        }

        @Test
        @DisplayName("should throw exception for null authorities")
        void shouldThrowExceptionForNullAuthorities() {
            assertThatThrownBy(() -> AuthorityValidator.validate((Set<String>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authorities required");
        }

        @Test
        @DisplayName("should throw exception for empty authorities")
        void shouldThrowExceptionForEmptyAuthorities() {
            assertThatThrownBy(() -> AuthorityValidator.validate(Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one authority required");
        }

        @Test
        @DisplayName("should throw exception for invalid authority string")
        void shouldThrowExceptionForInvalidAuthority() {
            assertThatThrownBy(() -> AuthorityValidator.validate(Set.of("INVALID:AUTHORITY")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid authority")
                    .hasMessageContaining("INVALID:AUTHORITY")
                    .hasMessageContaining("Valid authorities");
        }

        @Test
        @DisplayName("should throw exception for mixed valid and invalid authorities")
        void shouldThrowExceptionForMixedAuthorities() {
            assertThatThrownBy(() -> AuthorityValidator.validate(Set.of("MEMBERS:READ", "INVALID")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid authority");
        }

        @Test
        @DisplayName("should include valid authorities in error message")
        void shouldIncludeValidAuthoritiesInErrorMessage() {
            assertThatThrownBy(() -> AuthorityValidator.validate(Set.of("BAD")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MEMBERS:CREATE")
                    .hasMessageContaining("MEMBERS:READ");
        }
    }

    @Nested
    @DisplayName("validateAuthorityEnums(Set<Authority>) method")
    class ValidateAuthorityEnumsMethod {

        @Test
        @DisplayName("should accept valid authority enums")
        void shouldAcceptValidAuthorityEnums() {
            AuthorityValidator.validateAuthorityEnums(Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE));
        }

        @Test
        @DisplayName("should accept single authority enum")
        void shouldAcceptSingleAuthorityEnum() {
            AuthorityValidator.validateAuthorityEnums(Set.of(Authority.MEMBERS_READ));
        }

        @Test
        @DisplayName("should accept all authority enums")
        void shouldAcceptAllAuthorityEnums() {
            AuthorityValidator.validateAuthorityEnums(Set.of(Authority.values()));
        }

        @Test
        @DisplayName("should throw exception for null authority enums")
        void shouldThrowExceptionForNullAuthorityEnums() {
            assertThatThrownBy(() -> AuthorityValidator.validateAuthorityEnums(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authorities required");
        }

        @Test
        @DisplayName("should throw exception for empty authority enums")
        void shouldThrowExceptionForEmptyAuthorityEnums() {
            assertThatThrownBy(() -> AuthorityValidator.validateAuthorityEnums(Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one authority required");
        }
    }

    @Nested
    @DisplayName("Instantiation")
    class Instantiation {

        @Test
        @DisplayName("should not allow instantiation")
        void shouldNotAllowInstantiation() {
            assertThatThrownBy(() -> {
                var constructor = AuthorityValidator.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            })
                    .hasCauseInstanceOf(UnsupportedOperationException.class)
                    .getCause()
                    .hasMessageContaining("Utility class cannot be instantiated");
        }
    }
}
