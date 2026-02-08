package com.klabis.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Authority enum tests")
class AuthorityTest {

    @Nested
    @DisplayName("getValue() method")
    class GetValueMethod {

        @Test
        @DisplayName("should return correct string value for MEMBERS_CREATE")
        void shouldReturnValueForMembersCreate() {
            assertThat(Authority.MEMBERS_CREATE.getValue()).isEqualTo("MEMBERS:CREATE");
        }

        @Test
        @DisplayName("should return correct string value for MEMBERS_READ")
        void shouldReturnValueForMembersRead() {
            assertThat(Authority.MEMBERS_READ.getValue()).isEqualTo("MEMBERS:READ");
        }

        @Test
        @DisplayName("should return correct string value for MEMBERS_UPDATE")
        void shouldReturnValueForMembersUpdate() {
            assertThat(Authority.MEMBERS_UPDATE.getValue()).isEqualTo("MEMBERS:UPDATE");
        }

        @Test
        @DisplayName("should return correct string value for MEMBERS_DELETE")
        void shouldReturnValueForMembersDelete() {
            assertThat(Authority.MEMBERS_DELETE.getValue()).isEqualTo("MEMBERS:DELETE");
        }

        @Test
        @DisplayName("should return correct string value for MEMBERS_PERMISSIONS")
        void shouldReturnValueForMembersPermissions() {
            assertThat(Authority.MEMBERS_PERMISSIONS.getValue()).isEqualTo("MEMBERS:PERMISSIONS");
        }

        @Test
        @DisplayName("should return correct string value for EVENTS_MANAGE")
        void shouldReturnValueForEventsManage() {
            assertThat(Authority.EVENTS_MANAGE.getValue()).isEqualTo("EVENTS:MANAGE");
        }
    }

    @Nested
    @DisplayName("fromString() method")
    class FromStringMethod {

        @Test
        @DisplayName("should convert valid string to Authority enum")
        void shouldConvertValidStringToAuthority() {
            assertThat(Authority.fromString("MEMBERS:CREATE")).isEqualTo(Authority.MEMBERS_CREATE);
            assertThat(Authority.fromString("MEMBERS:READ")).isEqualTo(Authority.MEMBERS_READ);
            assertThat(Authority.fromString("MEMBERS:UPDATE")).isEqualTo(Authority.MEMBERS_UPDATE);
            assertThat(Authority.fromString("MEMBERS:DELETE")).isEqualTo(Authority.MEMBERS_DELETE);
            assertThat(Authority.fromString("MEMBERS:PERMISSIONS")).isEqualTo(Authority.MEMBERS_PERMISSIONS);
            assertThat(Authority.fromString("EVENTS:MANAGE")).isEqualTo(Authority.EVENTS_MANAGE);
        }

        @Test
        @DisplayName("should throw exception for invalid authority string")
        void shouldThrowExceptionForInvalidString() {
            assertThatThrownBy(() -> Authority.fromString("INVALID:AUTHORITY"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown authority");
        }

        @Test
        @DisplayName("should throw exception for null string")
        void shouldThrowExceptionForNullString() {
            // The fromString method throws IllegalArgumentException when value doesn't match
            assertThatThrownBy(() -> Authority.fromString(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown authority");
        }

        @Test
        @DisplayName("should throw exception for empty string")
        void shouldThrowExceptionForEmptyString() {
            assertThatThrownBy(() -> Authority.fromString(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown authority");
        }
    }

    @Nested
    @DisplayName("toString() method")
    class ToStringMethod {

        @Test
        @DisplayName("should return string value")
        void shouldReturnStringValue() {
            assertThat(Authority.MEMBERS_CREATE.toString()).isEqualTo("MEMBERS:CREATE");
            assertThat(Authority.MEMBERS_READ.toString()).isEqualTo("MEMBERS:READ");
        }
    }

    @Nested
    @DisplayName("getScope() method")
    class GetScopeMethod {

        @Test
        @DisplayName("should return CONTEXT_SPECIFIC scope for EVENTS_MANAGE")
        void shouldReturnContextSpecificScopeForEventsManage() {
            assertThat(Authority.EVENTS_MANAGE.getScope()).isEqualTo(Authority.Scope.CONTEXT_SPECIFIC);
        }

        @Test
        @DisplayName("should return CONTEXT_SPECIFIC scope for member authorities")
        void shouldReturnContextSpecificScopeForMemberAuthorities() {
            assertThat(Authority.MEMBERS_CREATE.getScope()).isEqualTo(Authority.Scope.CONTEXT_SPECIFIC);
            assertThat(Authority.MEMBERS_READ.getScope()).isEqualTo(Authority.Scope.CONTEXT_SPECIFIC);
            assertThat(Authority.MEMBERS_UPDATE.getScope()).isEqualTo(Authority.Scope.CONTEXT_SPECIFIC);
            assertThat(Authority.MEMBERS_DELETE.getScope()).isEqualTo(Authority.Scope.CONTEXT_SPECIFIC);
        }

        @Test
        @DisplayName("should return GLOBAL scope for MEMBERS_PERMISSIONS")
        void shouldReturnGlobalScopeForMembersPermissions() {
            assertThat(Authority.MEMBERS_PERMISSIONS.getScope()).isEqualTo(Authority.Scope.GLOBAL);
        }
    }

    @Nested
    @DisplayName("Enum values")
    class EnumValues {

        @Test
        @DisplayName("should have all 6 authority values")
        void shouldHaveSixAuthorityValues() {
            assertThat(Authority.values()).hasSize(6);
        }

        @Test
        @DisplayName("should contain all expected authorities")
        void shouldContainAllExpectedAuthorities() {
            assertThat(Authority.values()).containsExactlyInAnyOrder(
                    Authority.MEMBERS_CREATE,
                    Authority.MEMBERS_READ,
                    Authority.MEMBERS_UPDATE,
                    Authority.MEMBERS_DELETE,
                    Authority.MEMBERS_PERMISSIONS,
                    Authority.EVENTS_MANAGE
            );
        }

        @Test
        @DisplayName("valueOf() should return correct enum constant")
        void shouldReturnCorrectEnumConstant() {
            assertThat(Authority.valueOf("MEMBERS_CREATE")).isEqualTo(Authority.MEMBERS_CREATE);
            assertThat(Authority.valueOf("MEMBERS_READ")).isEqualTo(Authority.MEMBERS_READ);
        }
    }
}
