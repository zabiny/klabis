package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Common group error types")
class GroupErrorTypesTest {

    private static final MemberId MEMBER = new MemberId(UUID.randomUUID());

    @Test
    @DisplayName("membership errors name the group and keep the member for the handler to name")
    void membershipErrors() {
        Map<String, UserFacingError> expected = Map.of(
                "cannot-remove-last-owner", new CannotRemoveLastOwnerException(MEMBER, "Trenéři"),
                "owner-cannot-be-member", new OwnerCannotBeMemberException(MEMBER, "Trenéři"),
                "member-already-in-group", new MemberAlreadyInGroupException(MEMBER, "Trenéři"),
                "member-not-in-group", new MemberNotInGroupException(MEMBER, "Trenéři"));

        expected.forEach((type, error) -> {
            assertThat(error.errorType()).isEqualTo(type);
            assertThat(error.messageParameters()).containsOnly(Map.entry("groupName", "Trenéři"));
            assertThat(error.field()).isNull();
        });
        assertThat(new CannotRemoveLastOwnerException(MEMBER, "Trenéři").memberId()).isEqualTo(MEMBER);
        assertThat(new OwnerCannotBeMemberException(MEMBER, "Trenéři").memberId()).isEqualTo(MEMBER);
        assertThat(new MemberAlreadyInGroupException(MEMBER, "Trenéři").memberId()).isEqualTo(MEMBER);
        assertThat(new MemberNotInGroupException(MEMBER, "Trenéři").memberId()).isEqualTo(MEMBER);
        assertThat(new CannotRemoveLastOwnerException(MEMBER, "Trenéři")).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("direct member addition names the group")
    void directAddition() {
        DirectMemberAdditionNotAllowedException ex = new DirectMemberAdditionNotAllowedException("Oddíl");

        assertThat(ex.errorType()).isEqualTo("direct-member-addition-not-allowed");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("groupName", "Oddíl"));
    }

    @Test
    @DisplayName("an unknown group is a 404 without parameters, never naming the identifier")
    void groupNotFound() {
        GroupNotFoundException ex = new GroupNotFoundException("Training", UUID.randomUUID());

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.errorType()).isEqualTo("group-not-found");
        assertThat(ex.messageParameters()).isEmpty();
    }
}
