package com.klabis.members.legalguardian.domain;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LegalGuardian domain")
class LegalGuardianTest {

    private static final UserId USER_ID = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private static LegalGuardian aGuardian() {
        return LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                USER_ID, "Jan", "Novák", "jan.novak@example.com", "+420 777 123 456"));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("keeps the user id and the contact details")
        void keepsDetails() {
            LegalGuardian guardian = aGuardian();

            assertThat(guardian.getId()).isEqualTo(USER_ID);
            assertThat(guardian.getFirstName()).isEqualTo("Jan");
            assertThat(guardian.getLastName()).isEqualTo("Novák");
            assertThat(guardian.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(guardian.getPhone().value()).isEqualTo("+420 777 123 456");
        }

        @Test
        @DisplayName("requires an e-mail")
        void requiresEmail() {
            assertThatThrownBy(() -> LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                    USER_ID, "Jan", "Novák", null, "+420 777 123 456")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requires a phone")
        void requiresPhone() {
            assertThatThrownBy(() -> LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                    USER_ID, "Jan", "Novák", "jan.novak@example.com", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requires first and last name")
        void requiresNames() {
            assertThatThrownBy(() -> LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                    USER_ID, "", "Novák", "jan.novak@example.com", "+420 777 123 456")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                    USER_ID, "Jan", null, "jan.novak@example.com", "+420 777 123 456")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("changes only the supplied fields")
        void changesOnlySuppliedFields() {
            LegalGuardian guardian = aGuardian();

            guardian.update(new LegalGuardian.UpdateLegalGuardian(null, null, null, "+420 601 000 000"));

            assertThat(guardian.getPhone().value()).isEqualTo("+420 601 000 000");
            assertThat(guardian.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(guardian.getFirstName()).isEqualTo("Jan");
            assertThat(guardian.getLastName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("changes name and e-mail")
        void changesNameAndEmail() {
            LegalGuardian guardian = aGuardian();

            guardian.update(new LegalGuardian.UpdateLegalGuardian("Petr", "Dvořák", "petr@example.com", null));

            assertThat(guardian.getFirstName()).isEqualTo("Petr");
            assertThat(guardian.getLastName()).isEqualTo("Dvořák");
            assertThat(guardian.getEmail().value()).isEqualTo("petr@example.com");
        }

        @Test
        @DisplayName("cannot clear the e-mail")
        void cannotClearEmail() {
            LegalGuardian guardian = aGuardian();

            assertThatThrownBy(() -> guardian.update(new LegalGuardian.UpdateLegalGuardian(null, null, "  ", null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(guardian.getEmail().value()).isEqualTo("jan.novak@example.com");
        }

        @Test
        @DisplayName("cannot clear the phone")
        void cannotClearPhone() {
            LegalGuardian guardian = aGuardian();

            assertThatThrownBy(() -> guardian.update(new LegalGuardian.UpdateLegalGuardian(null, null, null, "")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(guardian.getPhone().value()).isEqualTo("+420 777 123 456");
        }
    }
}
