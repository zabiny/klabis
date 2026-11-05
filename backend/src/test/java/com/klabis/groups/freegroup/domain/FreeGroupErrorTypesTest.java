package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Free group error types")
class FreeGroupErrorTypesTest {

    private static final MemberId MEMBER = new MemberId(UUID.randomUUID());
    private static final String GROUP = "Pátečníci";

    @Test
    @DisplayName("invitation and promotion errors are bound to the member field and name the group")
    void memberFieldErrors() {
        Map<String, UserFacingError> expected = Map.of(
                "cannot-invite-existing-member", new CannotInviteExistingMemberException(MEMBER, GROUP),
                "duplicate-pending-invitation", new DuplicatePendingInvitationException(MEMBER, GROUP),
                "cannot-promote-non-member-to-owner", new CannotPromoteNonMemberToOwnerException(MEMBER, GROUP));

        expected.forEach((type, error) -> {
            assertThat(error.errorType()).isEqualTo(type);
            assertThat(error.field()).isEqualTo("memberId");
            assertThat(error.messageParameters()).containsOnly(Map.entry("groupName", GROUP));
        });
        assertThat(new CannotPromoteNonMemberToOwnerException(MEMBER, GROUP)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("bound fields exist as properties of the invite and add-owner requests")
    void boundFieldsAreRequestProperties() throws Exception {
        assertThat(recordComponents("com.klabis.groups.infrastructure.restapi.InviteMemberRequest")).contains("memberId");
        assertThat(recordComponents("com.klabis.groups.infrastructure.restapi.AddOwnerRequest")).contains("memberId");
    }

    @Test
    @DisplayName("a non-owner is refused with the group name, as an authorization error")
    void groupOwnershipRequired() {
        GroupOwnershipRequiredException ex = new GroupOwnershipRequiredException(MEMBER, new FreeGroupId(UUID.randomUUID()), GROUP);

        assertThat(ex).isInstanceOf(AuthorizationException.class);
        assertThat(ex.errorType()).isEqualTo("group-ownership-required");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("groupName", GROUP));
    }

    @Test
    @DisplayName("answering someone else's invitation names the group")
    void notInvitedMember() {
        NotInvitedMemberException ex = new NotInvitedMemberException(MEMBER, new InvitationId(UUID.randomUUID()), GROUP);

        assertThat(ex.errorType()).isEqualTo("not-invited-member");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("groupName", GROUP));
    }

    @Test
    @DisplayName("cancelling a non-pending invitation reports its status")
    void invitationNotCancellable() {
        InvitationNotCancellableException ex =
                new InvitationNotCancellableException(new InvitationId(UUID.randomUUID()), InvitationStatus.ACCEPTED);

        assertThat(ex.errorType()).isEqualTo("invitation-not-cancellable");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("invitationStatus", "ACCEPTED"));
    }

    @Test
    @DisplayName("delegating an authority that cannot be delegated reports the authority code")
    void invalidDelegatedAuthority() {
        InvalidDelegatedAuthorityException ex = new InvalidDelegatedAuthorityException(
                com.klabis.common.users.Authority.MEMBERS_PERMISSIONS);

        assertThat(ex.errorType()).isEqualTo("invalid-delegated-authority");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("authority", "MEMBERS:PERMISSIONS"));
    }

    @Test
    @DisplayName("a missing invitation is a 404 without parameters")
    void invitationNotFound() {
        InvitationNotFoundException ex = new InvitationNotFoundException(new InvitationId(UUID.randomUUID()));

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.errorType()).isEqualTo("invitation-not-found");
        assertThat(ex.messageParameters()).isEmpty();
    }

    private static Set<String> recordComponents(String className) throws ClassNotFoundException {
        return Set.copyOf(Arrays.stream(Class.forName(className).getRecordComponents())
                .map(RecordComponent::getName).toList());
    }
}
