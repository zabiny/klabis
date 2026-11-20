package com.klabis.finance.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountOwnerRelationshipSource")
class AccountOwnerRelationshipSourceTest {

    @Mock
    private Members members;

    @Test
    @DisplayName("grants FINANCE:VIEW over their own member id to a member")
    void shouldGrantMemberViewOverSelf() {
        UUID id = UUID.randomUUID();
        when(members.findById(new MemberId(id))).thenReturn(Optional.of(new MemberDto(id, "Jan", "Novák", null, "ZBM9000", null, null)));

        var grants = new AccountOwnerRelationshipSource(members).grantsOf(new UserId(id));

        assertThat(grants).containsOnlyKeys(Authority.FINANCE_VIEW);
        assertThat(grants.get(Authority.FINANCE_VIEW)).containsExactly(TargetRef.member(id));
    }

    @Test
    @DisplayName("grants nothing to a user who is not a member (EXT guardian, bootstrap admin)")
    void shouldGrantNothingToNonMember() {
        UUID id = UUID.randomUUID();
        when(members.findById(new MemberId(id))).thenReturn(Optional.empty());

        assertThat(new AccountOwnerRelationshipSource(members).grantsOf(new UserId(id))).isEmpty();
    }
}
