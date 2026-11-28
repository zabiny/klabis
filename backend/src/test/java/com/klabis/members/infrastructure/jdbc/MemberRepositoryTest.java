package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Member JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})  // jMolecules Repository annotation, used to load all repository adapters (for context caching)
)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM members.members")
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("round trip through the database")
    class RoundTrip {

        private RecursiveComparisonConfiguration persistedFields() {
            return RecursiveComparisonConfiguration.builder()
                    .withIgnoredFields("auditMetadata")
                    .withEqualsForType((a, b) -> Duration.between(a, b).abs().compareTo(Duration.ofMillis(1)) < 0,
                            Instant.class)
                    .build();
        }

        @Test
        @DisplayName("member with every field filled is read back unchanged")
        void shouldReadBackFullMemberUnchanged() {
            Member member = aMember()
                    .withName("Petra", "Nováková")
                    .withDateOfBirth(LocalDate.of(1995, 3, 1))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")))
                    .withEmail("petra.novakova@example.com")
                    .withPhone("+420111222333")
                    .withBirthNumber("950301/1234")
                    .withBankAccountNumber("123456/0300")
                    .withChipNumber("CHIP123")
                    .withIdentityCard(IdentityCard.of("AB123456", LocalDate.of(2099, 1, 1)))
                    .withMedicalCourse(MedicalCourse.of(LocalDate.of(2023, 1, 15), Optional.of(LocalDate.of(2025, 1, 15))))
                    .withTrainerLicense(TrainerLicense.of(TrainerLevel.T2, LocalDate.of(2098, 1, 1)))
                    .withRefereeLicense(RefereeLicense.of(RefereeLevel.R2, LocalDate.of(2097, 1, 1)))
                    .withDrivingLicenseGroup(DrivingLicenseGroup.A)
                    .withDietaryRestrictions("Vegetarian")
                    .withDataIncomplete(true)
                    .suspended(DeactivationReason.ODHLASKA, "Member requested termination")
                    .build();
            memberRepository.save(member);

            Optional<Member> loaded = memberRepository.findById(member.getId());

            assertThat(loaded).get()
                    .usingRecursiveComparison(persistedFields())
                    .isEqualTo(member);
        }

        @Test
        @DisplayName("member without address, contacts and optional fields is read back unchanged")
        void shouldReadBackMinimalMemberUnchanged() {
            Member member = aMember().withAddress(null).withEmail((EmailAddress) null).withPhone((PhoneNumber) null).build();
            memberRepository.save(member);

            Optional<Member> loaded = memberRepository.findById(member.getId());

            assertThat(loaded).get()
                    .usingRecursiveComparison(persistedFields())
                    .isEqualTo(member);
        }

        @Test
        @DisplayName("audit fields are filled on save and version starts at zero")
        void shouldFillAuditFieldsAndStartVersionAtZero() {
            Member member = aMember().build();
            memberRepository.save(member);

            Member loaded = memberRepository.findById(member.getId()).orElseThrow();

            assertThat(loaded.getCreatedAt()).isNotNull();
            assertThat(loaded.getCreatedBy()).isNotNull();
            assertThat(loaded.getLastModifiedAt()).isNotNull();
            assertThat(loaded.getLastModifiedBy()).isNotNull();
            assertThat(loaded.getVersion()).isZero();
        }
    }

    @Nested
    @DisplayName("data_incomplete column")
    class DataIncompleteFlag {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private boolean dataIncompleteColumnFor(UUID memberId) {
            Boolean value = jdbcTemplate.queryForObject(
                    "SELECT data_incomplete FROM members.members WHERE id = ?",
                    Boolean.class, memberId);
            return Boolean.TRUE.equals(value);
        }

        @Test
        @DisplayName("flips to false once a previously incomplete member is completed and re-saved")
        void shouldUpdateStoredFlagToFalseAfterCompletion() {
            Member incompleteMember = aMember()
                    .withRegistrationNumber("ZBM7003")
                    .withDataIncomplete(true)
                    .build();
            Member savedMember = memberRepository.save(incompleteMember);
            assertThat(dataIncompleteColumnFor(savedMember.getId().uuid())).isTrue();

            savedMember.recordMissingData(Set.of());
            Member resavedMember = memberRepository.save(savedMember);

            assertThat(dataIncompleteColumnFor(resavedMember.getId().uuid())).isFalse();
        }
    }

    @Nested
    @DisplayName("birth number encryption at rest")
    class BirthNumberEncryptionAtRest {

        private static final String PLAIN_BIRTH_NUMBER = "950301/1234";

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String rawBirthNumberColumnFor(UUID memberId) {
            return jdbcTemplate.queryForObject(
                    "SELECT birth_number FROM members.members WHERE id = ?",
                    String.class, memberId);
        }

        @Test
        @DisplayName("stored column value differs from the plain birth number and does not contain it")
        void storedColumnIsNotPlaintext() {
            Member member = memberRepository.save(aMember().withBirthNumber(PLAIN_BIRTH_NUMBER).build());

            String rawColumn = rawBirthNumberColumnFor(member.getId().uuid());

            assertThat(rawColumn)
                    .isNotNull()
                    .doesNotContain(PLAIN_BIRTH_NUMBER)
                    .doesNotContain("950301");
        }

        @Test
        @DisplayName("member without a birth number stores null")
        void absentBirthNumberStoresNull() {
            Member member = memberRepository.save(aMember().build());

            assertThat(rawBirthNumberColumnFor(member.getId().uuid())).isNull();
        }
    }

    @Nested
    @DisplayName("findAll(filter)")
    class FilterTests {

        @BeforeEach
        void setUp() {
            memberRepository.save(aMember().withRegistrationNumber("ZBM7001").withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1)).withActive(true).build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM7002").withName("Petr", "Čermák")
                    .withDateOfBirth(LocalDate.of(2001, 2, 2)).withActive(true).withDataIncomplete(true).build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM7003").withName("Jana", "Nováková")
                    .withDateOfBirth(LocalDate.of(2002, 3, 3)).withActive(false).build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM7004").withName("Karel", "Novak")
                    .withDateOfBirth(LocalDate.of(2003, 4, 4)).withActive(false).withDataIncomplete(true).build());
        }

        private static Arguments row(String description, MemberFilter filter, List<String> expected) {
            return arguments(Named.of(description, filter), expected);
        }

        static Stream<Arguments> filters() {
            return Stream.of(
                    row("no restriction returns everybody", MemberFilter.all(),
                            List.of("ZBM7001", "ZBM7002", "ZBM7003", "ZBM7004")),
                    row("status ACTIVE", MemberFilter.activeOnly(), List.of("ZBM7001", "ZBM7002")),
                    row("status INACTIVE", MemberFilter.all().withStatus(MemberFilter.StatusFilter.INACTIVE),
                            List.of("ZBM7003", "ZBM7004")),
                    row("fulltext matches first name substring", MemberFilter.all().withFulltext("Jan"),
                            List.of("ZBM7001", "ZBM7003")),
                    row("fulltext matches last name substring", MemberFilter.all().withFulltext("Nov"),
                            List.of("ZBM7001", "ZBM7003", "ZBM7004")),
                    row("fulltext matches registration number", MemberFilter.all().withFulltext("ZBM7004"),
                            List.of("ZBM7004")),
                    row("fulltext ignores case", MemberFilter.all().withFulltext("NOVAK"),
                            List.of("ZBM7001", "ZBM7003", "ZBM7004")),
                    row("fulltext without diacritics matches accented data", MemberFilter.all().withFulltext("cermak"),
                            List.of("ZBM7002")),
                    row("fulltext with diacritics matches unaccented data", MemberFilter.all().withFulltext("Novák"),
                            List.of("ZBM7001", "ZBM7003", "ZBM7004")),
                    row("fulltext tokens are ANDed", MemberFilter.all().withFulltext("karel novak"),
                            List.of("ZBM7004")),
                    row("fulltext with tokens matching different members finds nobody",
                            MemberFilter.all().withFulltext("jan cermak"), List.of()),
                    row("fulltext combined with status",
                            MemberFilter.all().withFulltext("novak").withStatus(MemberFilter.StatusFilter.INACTIVE),
                            List.of("ZBM7003", "ZBM7004")),
                    row("incompleteOnly", MemberFilter.all().withIncompleteOnly(true),
                            List.of("ZBM7002", "ZBM7004")),
                    row("incompleteOnly combined with status", MemberFilter.activeOnly().withIncompleteOnly(true),
                            List.of("ZBM7002")),
                    row("incompleteOnly combined with fulltext",
                            MemberFilter.all().withIncompleteOnly(true).withFulltext("Karel"), List.of("ZBM7004")),
                    row("bornOn keeps members born on any of the dates regardless of status",
                            MemberFilter.all().withBornOn(Set.of(LocalDate.of(2000, 1, 1), LocalDate.of(2002, 3, 3))),
                            List.of("ZBM7001", "ZBM7003")),
                    row("empty bornOn matches nobody", MemberFilter.all().withBornOn(Set.of()), List.of()),
                    row("bornOnOrBefore includes the boundary date",
                            MemberFilter.all().withBornOnOrBefore(LocalDate.of(2001, 2, 2)),
                            List.of("ZBM7001", "ZBM7002"))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("filters")
        void shouldReturnOnlyMembersMatchingFilter(MemberFilter filter, List<String> expected) {
            Page<Member> page = memberRepository.findAll(filter, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.getRegistrationNumber().getValue())
                    .containsExactlyInAnyOrderElementsOf(expected);
            assertThat(page.getTotalElements()).isEqualTo(expected.size());

            assertThat(memberRepository.findAll(filter))
                    .extracting(m -> m.getRegistrationNumber().getValue())
                    .containsExactlyInAnyOrderElementsOf(expected);

            Page<Member> unpaged = memberRepository.findAll(filter, Pageable.unpaged());
            assertThat(unpaged.getTotalElements()).isEqualTo(expected.size());
        }

        @Test
        @DisplayName("findDateOfBirth returns the date of birth of an existing member and empty for an unknown one")
        void shouldFindDateOfBirthWithoutLoadingTheAggregate() {
            Member saved = memberRepository.save(aMember().withRegistrationNumber("ZBM0801")
                    .withDateOfBirth(LocalDate.of(2008, 3, 1)).build());

            assertThat(memberRepository.findDateOfBirth(saved.getId())).contains(LocalDate.of(2008, 3, 1));
            assertThat(memberRepository.findDateOfBirth(new MemberId(UUID.randomUUID()))).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById() method")
    class FindByIdMethod {

        @Test
        @DisplayName("should return empty when member not found")
        void shouldReturnEmptyWhenMemberNotFound() {
            assertThat(memberRepository.findById(new MemberId(UUID.randomUUID()))).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRegistrationNumber() method")
    class FindByRegistrationIdMethod {

        @Test
        @DisplayName("should reject saving a second member with an already used registration number")
        void shouldRejectDuplicateRegistrationNumber() {
            RegistrationNumber regNum = new RegistrationNumber("ZBM0502");
            memberRepository.save(aMember().withRegistrationNumber(regNum).build());

            Member duplicate = aMember().withRegistrationNumber(regNum).build();

            assertThatThrownBy(() -> memberRepository.save(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should find member by registration number")
        void shouldFindMemberByRegistrationNumber() {
            RegistrationNumber regNum = new RegistrationNumber("ZBM0501");
            Member member = aMember().withRegistrationNumber(regNum).build();
            memberRepository.save(member);

            Optional<Member> foundMember = memberRepository.findByRegistrationNumber(regNum);

            assertThat(foundMember).map(Member::getId).contains(member.getId());
        }

        @Test
        @DisplayName("should return empty when registration number not found")
        void shouldReturnEmptyWhenRegistrationNumberNotFound() {
            Optional<Member> foundMember = memberRepository.findByRegistrationNumber(new RegistrationNumber("NON9999"));

            assertThat(foundMember).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail() method")
    class FindByEmailMethod {

        @Test
        @DisplayName("should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<Member> foundMember = memberRepository.findByEmail("nonexistent@example.com");

            assertThat(foundMember).isEmpty();
        }

        @Test
        @DisplayName("should be case-insensitive for email")
        void shouldBeCaseInsensitiveForEmail() {
            Member member = aMember().withEmail("test@example.com").build();
            memberRepository.save(member);

            Optional<Member> foundMember = memberRepository.findByEmail("TEST@EXAMPLE.COM");

            assertThat(foundMember).map(Member::getId).contains(member.getId());
        }
    }

    @Nested
    @DisplayName("countByBirthYear() method")
    class CountByBirthYearMethod {

        @Test
        @DisplayName("should count members by birth year correctly")
        void shouldCountMembersByBirthYear() {
            memberRepository.save(aMember().withRegistrationNumber("ZBM0501")
                    .withDateOfBirth(LocalDate.of(2005, 1, 1)).build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM0502")
                    .withDateOfBirth(LocalDate.of(2005, 12, 31)).build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM0601")
                    .withDateOfBirth(LocalDate.of(2006, 6, 15)).build());

            assertThat(memberRepository.countByBirthYear(2005)).isEqualTo(2);
            assertThat(memberRepository.countByBirthYear(2006)).isEqualTo(1);
            assertThat(memberRepository.countByBirthYear(2007)).isZero();
        }

        @Test
        @DisplayName("should return zero when no members exist")
        void shouldReturnZeroWhenNoMembersExist() {
            assertThat(memberRepository.countByBirthYear(2005)).isZero();
        }
    }

    @Nested
    @DisplayName("findAll() method - pagination and sorting")
    class FindAllMethod {

        @Test
        @DisplayName("should return paginated members")
        void shouldReturnPaginatedMembers() {
            for (int i = 1; i <= 5; i++) {
                memberRepository.save(aMember().withRegistrationNumber("ZBM000" + i).build());
            }

            Page<Member> page = memberRepository.findAll(MemberFilter.all(), PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("should sort members by last name ascending, then by first name ascending")
        void shouldSortMembersByLastName() {
            memberRepository.save(aMember().withRegistrationNumber("ZBM0501").withName("Karel", "Novak").build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM0201").withName("Petra", "Svoboda").build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM0001").withName("Jan", "Novak").build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM0301").withName("Adam", "Cerny").build());

            Pageable pageable = PageRequest.of(0, 10, Sort.by("lastName", "firstName").ascending());

            Page<Member> page = memberRepository.findAll(MemberFilter.all(), pageable);

            assertThat(page.getContent())
                    .extracting(Member::getLastName, Member::getFirstName)
                    .containsExactly(
                            tuple("Cerny", "Adam"),
                            tuple("Novak", "Jan"),
                            tuple("Novak", "Karel"),
                            tuple("Svoboda", "Petra"));
        }

        @Test
        @DisplayName("should return all members without pagination")
        void shouldReturnAllMembersWithoutPagination() {
            memberRepository.save(aMember().withRegistrationNumber("ZBM0501").build());
            memberRepository.save(aMember().withRegistrationNumber("ZBM1001").build());

            assertThat(memberRepository.findAll()).extracting(m -> m.getRegistrationNumber().getValue())
                    .containsExactlyInAnyOrder("ZBM0501", "ZBM1001");
        }

        @Test
        @DisplayName("should return empty list when no members exist")
        void shouldReturnEmptyListWhenNoMembersExist() {
            assertThat(memberRepository.findAll()).isEmpty();
        }
    }
}
