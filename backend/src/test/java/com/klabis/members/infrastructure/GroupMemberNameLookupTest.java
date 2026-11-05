package com.klabis.members.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberDtoBuilder;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GroupMemberNameLookup")
class GroupMemberNameLookupTest {

    private static final UUID UUID_OF_MEMBER = UUID.randomUUID();

    private final Members members = mock(Members.class);
    private final GroupMemberNameLookup lookup = new GroupMemberNameLookup(members);

    @Test
    @DisplayName("names a member by first and last name for a member id and for a user id")
    void namesMemberById() {
        when(members.findById(new MemberId(UUID_OF_MEMBER))).thenReturn(Optional.of(
                MemberDtoBuilder.builder().memberId(UUID_OF_MEMBER).firstName("Jan").lastName("Novák").build()));

        assertThat(lookup.nameOf(new MemberId(UUID_OF_MEMBER))).contains("Jan Novák");
        assertThat(lookup.nameOf(new UserId(UUID_OF_MEMBER))).contains("Jan Novák");
    }

    @Test
    @DisplayName("leaves out a person who is not a member and an identifier of an unknown kind")
    void unknownPerson() {
        when(members.findById(new MemberId(UUID_OF_MEMBER))).thenReturn(Optional.empty());

        assertThat(lookup.nameOf(new UserId(UUID_OF_MEMBER))).isEmpty();
        assertThat(lookup.nameOf("not an id")).isEmpty();
    }
}
