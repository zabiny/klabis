package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.legalguardiangroup.application.InMemoryLegalGuardianGroupRepository;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LegalGuardianGroupSuspensionBlockersAdapter")
class LegalGuardianGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final Minor CHILD = new Minor(new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            LocalDate.now().minusYears(9));

    private InMemoryLegalGuardianGroupRepository repository;

    private LegalGuardianGroupSuspensionBlockersAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLegalGuardianGroupRepository();
        adapter = new LegalGuardianGroupSuspensionBlockersAdapter(repository);
    }

    private static Guardian guardian(MemberId member) {
        return new Guardian(member.toUserId(), "Novák");
    }

    @Test
    @DisplayName("reports the group when the member is the sole guardian of its minors")
    void reportsGroupWhenMemberIsSoleGuardian() {
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(guardian(MEMBER)), CHILD);
        repository.save(group);

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).containsExactly(
                new OwnedGroup(group.getId().uuid().toString(), group.getName(), LegalGuardianGroup.TYPE_DISCRIMINATOR,
                        URI.create("/api/legal-guardian-groups/" + group.getId().uuid())));
    }

    @Test
    @DisplayName("reports every group where the member is the sole guardian")
    void reportsEveryGroup() {
        LegalGuardianGroup first = LegalGuardianGroup.create(Set.of(guardian(MEMBER)), CHILD);
        LegalGuardianGroup second = LegalGuardianGroup.create(Set.of(guardian(MEMBER)),
                new Minor(new MemberId(UUID.randomUUID()), LocalDate.now().minusYears(4)));
        repository.save(first);
        repository.save(second);

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).hasSize(2);
    }

    @Test
    @DisplayName("does not report a group with another guardian")
    void ignoresGroupWithCoGuardian() {
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(guardian(MEMBER), guardian(OTHER)), CHILD);
        repository.save(group);

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list when the member guards nobody")
    void returnsEmptyWhenNoGroups() {
        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("does not report a group guarded only by somebody else")
    void ignoresGroupOfAnotherGuardian() {
        repository.save(LegalGuardianGroup.create(Set.of(guardian(OTHER)), CHILD));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }
}
