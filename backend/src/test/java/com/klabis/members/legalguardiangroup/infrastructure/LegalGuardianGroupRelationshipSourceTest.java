package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LegalGuardianGroupRelationshipSource")
class LegalGuardianGroupRelationshipSourceTest {

    private final GroupsStub groups = new GroupsStub();
    private final LegalGuardianGroupRelationshipSource source = new LegalGuardianGroupRelationshipSource(groups);

    private static final Guardian ANNA = new Guardian(new UserId(UUID.randomUUID()), "Nováková");
    private static final Guardian BOHUMIL = new Guardian(new UserId(UUID.randomUUID()), "Dvořák");

    private static Minor aMinor() {
        return new Minor(new MemberId(UUID.randomUUID()), LocalDate.now().minusYears(10));
    }

    private static TargetRef member(Minor minor) {
        return TargetRef.member(minor.id().uuid());
    }

    @Test
    @DisplayName("grants a guardian profile editing over each minor of their group")
    void shouldGrantGuardianOverEachMinor() {
        Minor first = aMinor();
        Minor second = aMinor();
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(ANNA), first);
        group.addMinor(second);
        groups.save(group);

        var grants = source.grantsOf(ANNA.userId());

        assertThat(grants).containsOnlyKeys(Authority.MEMBERS_EDIT_PROFILE);
        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactlyInAnyOrder(member(first), member(second));
    }

    @Test
    @DisplayName("grants a guardian of several groups the union of their minors")
    void shouldGrantUnionOverAllGroupsOfGuardian() {
        Minor ofAnnaAlone = aMinor();
        Minor ofBoth = aMinor();
        groups.save(LegalGuardianGroup.create(Set.of(ANNA), ofAnnaAlone));
        groups.save(LegalGuardianGroup.create(Set.of(ANNA, BOHUMIL), ofBoth));

        var grants = source.grantsOf(ANNA.userId());

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactlyInAnyOrder(member(ofAnnaAlone), member(ofBoth));
    }

    @Test
    @DisplayName("grants nothing over minors of groups the user does not guard")
    void shouldNotGrantOverOtherMinors() {
        Minor annasChild = aMinor();
        Minor bohumilsChild = aMinor();
        groups.save(LegalGuardianGroup.create(Set.of(ANNA), annasChild));
        groups.save(LegalGuardianGroup.create(Set.of(BOHUMIL), bohumilsChild));

        var grants = source.grantsOf(ANNA.userId());

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(member(annasChild));
    }

    @Test
    @DisplayName("grants a removed guardian nothing")
    void shouldGrantNothingToRemovedGuardian() {
        Minor minor = aMinor();
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(ANNA, BOHUMIL), minor);
        group.changeGuardians(Set.of(BOHUMIL));
        groups.save(group);

        assertThat(source.grantsOf(ANNA.userId())).isEmpty();
        assertThat(source.grantsOf(BOHUMIL.userId()).get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(member(minor));
    }

    @Test
    @DisplayName("grants nothing over a minor who left the group")
    void shouldGrantNothingOverMinorWhoLeft() {
        Minor staying = aMinor();
        Minor leaving = aMinor();
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(ANNA), staying);
        group.addMinor(leaving);
        group.removeMinor(leaving.id());
        groups.save(group);

        var grants = source.grantsOf(ANNA.userId());

        assertThat(grants.get(Authority.MEMBERS_EDIT_PROFILE)).containsExactly(member(staying));
    }

    @Test
    @DisplayName("grants nothing to a guardian whose group has no minors left")
    void shouldGrantNothingWhenNoMinorsLeft() {
        Minor leaving = aMinor();
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(ANNA), leaving);
        group.removeMinor(leaving.id());
        groups.save(group);

        assertThat(source.grantsOf(ANNA.userId())).isEmpty();
    }

    @Test
    @DisplayName("grants nothing to a user who guards nobody")
    void shouldGrantNothingToNonGuardian() {
        groups.save(LegalGuardianGroup.create(Set.of(ANNA), aMinor()));

        assertThat(source.grantsOf(new UserId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @DisplayName("grants nothing over the user's own membership in a group as a minor")
    void shouldNotGrantToMinorOfGroup() {
        Minor minor = aMinor();
        groups.save(LegalGuardianGroup.create(Set.of(ANNA), minor));

        assertThat(source.grantsOf(minor.id().toUserId())).isEmpty();
    }

    private static class GroupsStub implements LegalGuardianGroupRepository {

        private final List<LegalGuardianGroup> stored = new ArrayList<>();

        @Override
        public LegalGuardianGroup save(LegalGuardianGroup group) {
            stored.add(group);
            return group;
        }

        @Override
        public Optional<LegalGuardianGroup> findById(LegalGuardianGroupId id) {
            return stored.stream().filter(group -> group.getId().equals(id)).findFirst();
        }

        @Override
        public List<LegalGuardianGroup> findAll(LegalGuardianGroupFilter filter) {
            return stored.stream()
                    .filter(group -> filter.guardianIs() == null || group.isOwner(filter.guardianIs()))
                    .toList();
        }

        @Override
        public Optional<LegalGuardianGroup> findOne(LegalGuardianGroupFilter filter) {
            return findAll(filter).stream().findFirst();
        }

        @Override
        public void delete(LegalGuardianGroupId id) {
            stored.removeIf(group -> group.getId().equals(id));
        }
    }
}
