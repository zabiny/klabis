package com.klabis.members;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.UserId;
import com.klabis.members.application.AccountActivationNotAvailableException;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.SuspensionBlockedException;
import com.klabis.members.legalguardian.application.LegalGuardianNotFoundException;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupWithoutGuardianException;
import com.klabis.members.legalguardiangroup.domain.OnlyMinorsAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Members error types")
class MembersErrorTypesTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());

    private static void assertError(UserFacingError error, String type, Map<String, Object> parameters) {
        assertThat(error.errorType()).isEqualTo(type);
        assertThat(error.messageParameters()).containsExactlyInAnyOrderEntriesOf(parameters);
        assertThat(error.field()).isNull();
        assertThat(error.messageParameters().values().toString()).doesNotContain(MEMBER_ID.uuid().toString());
    }

    @Test
    @DisplayName("not found errors carry no identifier")
    void notFoundErrors() {
        assertError(new MemberNotFoundException(MEMBER_ID), "member-not-found", Map.of());
        assertError(new LegalGuardianNotFoundException(new UserId(MEMBER_ID.uuid())), "legal-guardian-not-found",
                Map.of());
    }

    @Test
    @DisplayName("legal guardian rules")
    void legalGuardianRules() {
        assertError(new LegalGuardianEmailAlreadyInUseException("a@b.cz"), "legal-guardian-email-already-in-use",
                Map.of("email", "a@b.cz"));
        assertError(new GuardianNotFoundException(new UserId(MEMBER_ID.uuid())), "guardian-not-found", Map.of());
        assertError(new LegalGuardianGroupWithoutGuardianException(), "legal-guardian-group-without-guardian", Map.of());
        assertError(new OnlyMinorsAllowedException(MEMBER_ID), "only-minors-allowed", Map.of());
    }

    @Test
    @DisplayName("account activation")
    void accountActivation() {
        assertError(new AccountActivationNotAvailableException(MEMBER_ID), "account-activation-not-available", Map.of());
    }

    @Test
    @DisplayName("suspension blocked")
    void suspensionBlocked() {
        SuspensionBlockedException ex = new SuspensionBlockedException(List.of(), null);

        assertThat(ex.errorType()).isEqualTo("suspension-blocked");
    }
}
