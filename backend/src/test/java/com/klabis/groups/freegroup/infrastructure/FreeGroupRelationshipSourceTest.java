package com.klabis.groups.freegroup.infrastructure;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FreeGroupRelationshipSource")
class FreeGroupRelationshipSourceTest {

    private static final Set<Authority> EDIT_PROFILE = Set.of(Authority.MEMBERS_EDIT_PROFILE);

    private final GroupsStub groups = new GroupsStub();
    private final FreeGroupRelationshipSource source = new FreeGroupRelationshipSource(groups);

    private static final MemberId OWNER = memberId();
    private static final MemberId CO_OWNER = memberId();

    private static MemberId memberId() {
        return new MemberId(UUID.randomUUID());
    }

    private static TargetRef target(MemberId member) {
        return TargetRef.member(member.uuid());
    }

    private static UserId user(MemberId member) {
        return member.toUserId();
    }

    private FreeGroup groupWithMembers(MemberId owner, Set<Authority> delegated, MemberId... members) {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Group", owner, delegated));
        for (MemberId member : members) {
            group.invite(owner, member);
            group.acceptInvitation(group.getPendingInvitations().getFirst().getId());
        }
        groups.save(group);
        return group;
    }

    @Test
    @DisplayName("grants an owner the delegated authority over each member of the group")
    void shouldGrantOwnerOverEachMember() {
        MemberId first = memberId();
        MemberId second = memberId();
        groupWithMembers(OWNER, EDIT_PROFILE, first, second);

        var grants = source.grantsOf(user(OWNER));

        assertThat(grants).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE);
        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE))
                .containsExactlyInAnyOrder(target(first), target(second));
    }

    @Test
    @DisplayName("grants nothing over co-owners, who are not members")
    void shouldNotGrantOverCoOwners() {
        MemberId member = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE, member, CO_OWNER);
        group.addOwner(CO_OWNER, OWNER);

        var grants = source.grantsOf(user(OWNER));

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(target(member));
    }

    @Test
    @DisplayName("grants a co-owner the same authority over the members")
    void shouldGrantCoOwner() {
        MemberId member = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE, member, CO_OWNER);
        group.addOwner(CO_OWNER, OWNER);

        var grants = source.grantsOf(user(CO_OWNER));

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(target(member));
    }

    @Test
    @DisplayName("grants a member nothing over the other members")
    void shouldNotGrantMembers() {
        MemberId member = memberId();
        MemberId other = memberId();
        groupWithMembers(OWNER, EDIT_PROFILE, member, other);

        assertThat(source.grantsOf(user(member))).isEmpty();
    }

    @Test
    @DisplayName("grants nothing over a member who left the group")
    void shouldDropMemberWhoLeft() {
        MemberId staying = memberId();
        MemberId leaving = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE, staying, leaving);
        group.removeMember(leaving, leaving);

        var grants = source.grantsOf(user(OWNER));

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(target(staying));
    }

    @Test
    @DisplayName("grants nothing over a member removed by an owner")
    void shouldDropMemberRemovedByOwner() {
        MemberId removed = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE, removed);
        group.removeMember(removed, OWNER);

        assertThat(source.grantsOf(user(OWNER))).isEmpty();
    }

    @Test
    @DisplayName("grants nothing over an invited member who has not accepted yet")
    void shouldNotGrantOverPendingInvitee() {
        MemberId invitee = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE);
        group.invite(OWNER, invitee);
        groups.save(group);

        assertThat(source.grantsOf(user(OWNER))).isEmpty();
    }

    @Test
    @DisplayName("grants a removed owner nothing")
    void shouldGrantNothingToRemovedOwner() {
        MemberId member = memberId();
        FreeGroup group = groupWithMembers(OWNER, EDIT_PROFILE, member, CO_OWNER);
        group.addOwner(CO_OWNER, OWNER);
        group.removeOwner(OWNER, CO_OWNER);

        assertThat(source.grantsOf(user(OWNER))).isEmpty();
        assertThat(source.grantsOf(user(CO_OWNER))).isNotEmpty();
    }

    @Test
    @DisplayName("grants nothing when the group delegates nothing")
    void shouldGrantNothingWhenNothingDelegated() {
        groupWithMembers(OWNER, Set.of(), memberId());

        assertThat(source.grantsOf(user(OWNER))).isEmpty();
    }

    @Test
    @DisplayName("grants nothing for a group without members")
    void shouldGrantNothingForGroupWithoutMembers() {
        groupWithMembers(OWNER, EDIT_PROFILE);

        assertThat(source.grantsOf(user(OWNER))).isEmpty();
    }

    @Test
    @DisplayName("grants the union over all groups the user owns")
    void shouldUniteGroupsOfOwner() {
        MemberId inFirst = memberId();
        MemberId inBoth = memberId();
        MemberId inSecond = memberId();
        groupWithMembers(OWNER, EDIT_PROFILE, inFirst, inBoth);
        groupWithMembers(OWNER, EDIT_PROFILE, inBoth, inSecond);

        var grants = source.grantsOf(user(OWNER));

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE))
                .containsExactlyInAnyOrder(target(inFirst), target(inBoth), target(inSecond));
    }

    @Test
    @DisplayName("grants only from groups that delegate when the user owns delegating and plain groups")
    void shouldIgnoreGroupsDelegatingNothing() {
        MemberId delegated = memberId();
        MemberId plain = memberId();
        groupWithMembers(OWNER, EDIT_PROFILE, delegated);
        groupWithMembers(OWNER, Set.of(), plain);

        var grants = source.grantsOf(user(OWNER));

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(target(delegated));
    }

    @Test
    @DisplayName("grants nothing to a user who owns no group")
    void shouldGrantNothingToNonOwner() {
        groupWithMembers(OWNER, EDIT_PROFILE, memberId());

        assertThat(source.grantsOf(new UserId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @DisplayName("unites with grants from another relationship source in the authorization snapshot")
    void shouldUniteWithGrantsOfOtherSources() {
        MemberId groupMember = memberId();
        TargetRef guardedChild = TargetRef.member(UUID.randomUUID());
        groupWithMembers(OWNER, EDIT_PROFILE, groupMember);
        Map<Authority, Set<TargetRef>> guardianGrants = Map.of(Authority.MEMBERS_EDIT_PROFILE, Set.of(guardedChild));

        AuthorizationSnapshot snapshot = AuthorizationSnapshot.fromSources(Set.of(),
                List.of(source.grantsOf(user(OWNER)), guardianGrants));

        assertThat(snapshot.has(Authority.MEMBERS_EDIT_PROFILE, target(groupMember))).isTrue();
        assertThat(snapshot.has(Authority.MEMBERS_EDIT_PROFILE, guardedChild)).isTrue();
    }

    private static class GroupsStub implements FreeGroupRepository {

        private final List<FreeGroup> stored = new ArrayList<>();

        @Override
        public FreeGroup save(FreeGroup group) {
            if (!stored.contains(group)) {
                stored.add(group);
            }
            return group;
        }

        @Override
        public Optional<FreeGroup> findById(FreeGroupId id) {
            return stored.stream().filter(group -> group.getId().equals(id)).findFirst();
        }

        @Override
        public List<FreeGroup> findAll(FreeGroupFilter filter) {
            return stored.stream()
                    .filter(group -> filter.ownerIs() == null || group.isOwner(filter.ownerIs()))
                    .filter(group -> filter.ownerOrMemberIs() == null
                                     || group.isOwner(filter.ownerOrMemberIs())
                                     || group.hasMember(filter.ownerOrMemberIs()))
                    .toList();
        }

        @Override
        public Optional<FreeGroup> findOne(FreeGroupFilter filter) {
            return findAll(filter).stream().findFirst();
        }

        @Override
        public boolean exists(FreeGroupFilter filter) {
            return !findAll(filter).isEmpty();
        }

        @Override
        public boolean existsById(FreeGroupId id) {
            return findById(id).isPresent();
        }

        @Override
        public void delete(FreeGroupId id) {
            stored.removeIf(group -> group.getId().equals(id));
        }
    }
}
