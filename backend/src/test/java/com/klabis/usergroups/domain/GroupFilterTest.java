package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GroupFilter domain unit tests")
class GroupFilterTest {

    private static final UserGroupId GROUP_ID = new UserGroupId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Nested
    @DisplayName("GroupFilter.all()")
    class AllFilter {

        @Test
        @DisplayName("should have all fields null")
        void shouldHaveAllFieldsNull() {
            GroupFilter filter = GroupFilter.all();

            assertThat(filter.id()).isNull();
            assertThat(filter.type()).isNull();
            assertThat(filter.owner()).isNull();
            assertThat(filter.member()).isNull();
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }

    @Nested
    @DisplayName("GroupFilter.byId()")
    class ByIdFilter {

        @Test
        @DisplayName("should set id and leave all other fields null")
        void shouldSetIdAndLeaveOthersNull() {
            GroupFilter filter = GroupFilter.byId(GROUP_ID);

            assertThat(filter.id()).isEqualTo(GROUP_ID);
            assertThat(filter.type()).isNull();
            assertThat(filter.owner()).isNull();
            assertThat(filter.member()).isNull();
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }

    @Nested
    @DisplayName("GroupFilter.byMember()")
    class ByMemberFilter {

        @Test
        @DisplayName("should set member and leave all other fields null")
        void shouldSetMemberAndLeaveOthersNull() {
            GroupFilter filter = GroupFilter.byMember(MEMBER_ID);

            assertThat(filter.id()).isNull();
            assertThat(filter.type()).isNull();
            assertThat(filter.owner()).isNull();
            assertThat(filter.member()).isEqualTo(MEMBER_ID);
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }

    @Nested
    @DisplayName("GroupFilter.byOwner()")
    class ByOwnerFilter {

        @Test
        @DisplayName("should set owner and leave all other fields null")
        void shouldSetOwnerAndLeaveOthersNull() {
            GroupFilter filter = GroupFilter.byOwner(MEMBER_ID);

            assertThat(filter.id()).isNull();
            assertThat(filter.type()).isNull();
            assertThat(filter.owner()).isEqualTo(MEMBER_ID);
            assertThat(filter.member()).isNull();
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }

    @Nested
    @DisplayName("GroupFilter.byType()")
    class ByTypeFilter {

        @Test
        @DisplayName("should set type and leave all other fields null")
        void shouldSetTypeAndLeaveOthersNull() {
            GroupFilter filter = GroupFilter.byType(GroupType.TRAINING);

            assertThat(filter.id()).isNull();
            assertThat(filter.type()).isEqualTo(GroupType.TRAINING);
            assertThat(filter.owner()).isNull();
            assertThat(filter.member()).isNull();
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }

    @Nested
    @DisplayName("GroupFilter.byTypeAndMember()")
    class ByTypeAndMemberFilter {

        @Test
        @DisplayName("should set type and member and leave all other fields null")
        void shouldSetTypeAndMemberAndLeaveOthersNull() {
            GroupFilter filter = GroupFilter.byTypeAndMember(GroupType.FAMILY, MEMBER_ID);

            assertThat(filter.id()).isNull();
            assertThat(filter.type()).isEqualTo(GroupType.FAMILY);
            assertThat(filter.owner()).isNull();
            assertThat(filter.member()).isEqualTo(MEMBER_ID);
            assertThat(filter.pendingInvitationForMember()).isNull();
        }
    }
}
