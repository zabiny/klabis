package com.klabis.members.legalguardiangroup.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LegalGuardianGroup JDBC Persistence Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class LegalGuardianGroupPersistenceTest {

    @Autowired
    private LegalGuardianGroupRepository repository;

    // No rows in members.members — groups reference guardians by id only (a guardian need not be a member).
    private static final Guardian NOVAK = new Guardian(new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111")), "Novák");
    private static final Guardian SVOBODOVA = new Guardian(new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222")), "Svobodová");
    private static final Minor CHILD_A = minor("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Minor CHILD_B = minor("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static Minor minor(String uuid) {
        return new Minor(new MemberId(UUID.fromString(uuid)), LocalDate.now().minusYears(10));
    }

    @Nested
    @DisplayName("save() and findById()")
    class SaveAndFindById {

        @Test
        @DisplayName("round-trips guardians, minors and the generated name")
        void roundTrip() {
            LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(NOVAK, SVOBODOVA), CHILD_A);
            group.addMinor(CHILD_B);

            repository.save(group);
            Optional<LegalGuardianGroup> found = repository.findById(group.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Novák a Svobodová");
            assertThat(found.get().getGuardians()).containsExactlyInAnyOrder(NOVAK.userId(), SVOBODOVA.userId());
            assertThat(found.get().getMinors()).extracting(m -> m.memberId())
                    .containsExactlyInAnyOrder(CHILD_A.id(), CHILD_B.id());
            assertThat(found.get().getAuditMetadata()).isNotNull();
        }

        @Test
        @DisplayName("persists in-place change of guardians")
        void persistsGuardianChange() {
            LegalGuardianGroup group = repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));

            group.changeGuardians(Set.of(SVOBODOVA));
            repository.save(group);

            LegalGuardianGroup found = repository.findById(group.getId()).orElseThrow();
            assertThat(found.getGuardians()).containsExactly(SVOBODOVA.userId());
            assertThat(found.getName()).isEqualTo("Svobodová");
        }
    }

    @Nested
    @DisplayName("findAll() / findOne()")
    class Queries {

        @Test
        @DisplayName("guardianIs finds every group of a guardian representing children in several groups")
        void guardianInSeveralGroups() {
            repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));
            repository.save(LegalGuardianGroup.create(Set.of(NOVAK, SVOBODOVA), CHILD_B));

            List<LegalGuardianGroup> groups = repository.findAll(LegalGuardianGroupFilter.all().withGuardianIs(NOVAK.userId()));

            assertThat(groups).hasSize(2);
        }

        @Test
        @DisplayName("minorIs finds the group of the child, guardian is not a minor of any group")
        void minorIs() {
            LegalGuardianGroup group = repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));

            assertThat(repository.findOne(LegalGuardianGroupFilter.all().withMinorIs(CHILD_A.id().toUserId())))
                    .map(LegalGuardianGroup::getId).contains(group.getId());
            assertThat(repository.findOne(LegalGuardianGroupFilter.all().withMinorIs(NOVAK.userId()))).isEmpty();
        }

        @Test
        @DisplayName("guardiansAre matches the exact set only, not a subset or superset")
        void guardiansAreExact() {
            LegalGuardianGroup single = repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));
            LegalGuardianGroup pair = repository.save(LegalGuardianGroup.create(Set.of(NOVAK, SVOBODOVA), CHILD_B));

            assertThat(repository.findOne(LegalGuardianGroupFilter.all().withGuardiansAre(Set.of(NOVAK.userId()))))
                    .map(LegalGuardianGroup::getId).contains(single.getId());
            assertThat(repository.findOne(LegalGuardianGroupFilter.all()
                    .withGuardiansAre(Set.of(NOVAK.userId(), SVOBODOVA.userId()))))
                    .map(LegalGuardianGroup::getId).contains(pair.getId());
            assertThat(repository.findOne(LegalGuardianGroupFilter.all().withGuardiansAre(Set.of(SVOBODOVA.userId())))).isEmpty();
        }

        @Test
        @DisplayName("all() returns every legal guardian group")
        void all() {
            repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));
            repository.save(LegalGuardianGroup.create(Set.of(SVOBODOVA), CHILD_B));

            assertThat(repository.findAll(LegalGuardianGroupFilter.all())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("removes the group")
        void deletes() {
            LegalGuardianGroup group = repository.save(LegalGuardianGroup.create(Set.of(NOVAK), CHILD_A));

            repository.delete(group.getId());

            assertThat(repository.findById(group.getId())).isEmpty();
        }
    }
}
