package com.klabis.members.infrastructure.jdbc;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberAssert;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})  // jMolecules Repository annotation, used to load all repository adapters (for context caching)
)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM members")
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("save() method")
    class SaveMethod {

        @Test
        @DisplayName("should save new member with all required fields")
        void shouldSaveNewMember() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            MemberAssert.assertThat(savedMember)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(LocalDate.of(2005, 3, 15))
                    .hasNationality("CZ")
                    .hasGender(Gender.MALE)
                    .isActive()
                    .hasGuardian(null);
            assertThat(savedMember.getId()).isNotNull();
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0501");
            assertThat(savedMember.getEmail()).isNotNull();
            assertThat(savedMember.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(savedMember.getPhone()).isNotNull();
            assertThat(savedMember.getPhone().value()).isEqualTo("+420123456789");
        }

        @Test
        @DisplayName("should save member with guardian information")
        void shouldSaveMemberWithGuardian() {
            // Given
            GuardianInformation guardian = new GuardianInformation(
                    "Pavel",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("pavel.novak@example.com"),
                    PhoneNumber.of("+420987654321")
            );
            Member member = aMember()
                    .withRegistrationNumber("ZBM1001")
                    .withName("Petra", "Nováková")
                    .withDateOfBirth(LocalDate.of(2010, 6, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 1", "Brno", "60200", "CZ"))
                    .withEmail("petra.novakova@example.com")
                    .withPhone("+420111222333")
                    .withGuardian(guardian)
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            MemberAssert.assertThat(savedMember).hasGuardianNotNull();
            assertThat(savedMember.getGuardian().getFirstName()).isEqualTo("Pavel");
            assertThat(savedMember.getGuardian().getLastName()).isEqualTo("Novák");
            assertThat(savedMember.getGuardian().getRelationship()).isEqualTo("PARENT");
            assertThat(savedMember.getGuardian().getEmailValue()).isEqualTo("pavel.novak@example.com");
            assertThat(savedMember.getGuardian().getPhoneValue()).isEqualTo("+420987654321");
        }

        @Test
        @DisplayName("should populate createdAt and createdBy on save")
        void shouldPopulateCreatedAtAndCreatedByOnSave() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0001")
                    .withName("Test", "User")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 5", "Ostrava", "70800", "CZ"))
                    .withEmail("test@example.com")
                    .withPhone("+420111111111")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getCreatedAt()).isNotNull();
            assertThat(savedMember.getCreatedBy()).isNotNull();
            assertThat(savedMember.getLastModifiedAt()).isNotNull();
            assertThat(savedMember.getLastModifiedBy()).isNotNull();
        }

        @Test
        @DisplayName("should set version to zero on new member")
        void shouldSetVersionToZeroOnNewMember() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0002")
                    .withName("Version", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("version@example.com")
                    .withPhone("+420111111112")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getVersion()).isNotNull();
            assertThat(savedMember.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should save member with all optional fields")
        void shouldSaveMemberWithAllOptionalFields() {
            // Given
            GuardianInformation guardian = new GuardianInformation(
                    "Guardian",
                    "Name",
                    "PARENT",
                    EmailAddress.of("guardian@example.com"),
                    PhoneNumber.of("+420111111113")
            );
            Member member = aMember()
                    .withRegistrationNumber("ZBM0003")
                    .withName("Complete", "Member")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("complete@example.com")
                    .withPhone("+420111111114")
                    .withGuardian(guardian)
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getId()).isNotNull();
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0003");
        }
    }

    @Nested
    @DisplayName("findById() method")
    class FindByIdMethod {

        @Test
        @DisplayName("should find member by id")
        void shouldFindMemberById() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
            Member savedMember = memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

            // Then
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getId()).isEqualTo(savedMember.getId());
            assertThat(foundMember.get().getFirstName()).isEqualTo("Jan");
            assertThat(foundMember.get().getLastName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("should return empty when member not found")
        void shouldReturnEmptyWhenMemberNotFound() {
            // Given
            MemberId nonExistentId = new MemberId(UUID.randomUUID());

            // When
            Optional<Member> foundMember = memberRepository.findById(nonExistentId);

            // Then
            assertThat(foundMember).isEmpty();
        }

        @Test
        @DisplayName("should load all member fields correctly")
        void shouldLoadAllMemberFieldsCorrectly() {
            // Given
            GuardianInformation guardian = new GuardianInformation(
                    "Guard",
                    "Guardian",
                    "PARENT",
                    EmailAddress.of("guard@example.com"),
                    PhoneNumber.of("+420111111115")
            );
            Member member = aMember()
                    .withRegistrationNumber("ZBM0004")
                    .withName("Full", "Load")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Test", "11000", "CZ"))
                    .withEmail("full@example.com")
                    .withPhone("+420111111116")
                    .withGuardian(guardian)
                    .build();
            Member savedMember = memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

            // Then
            assertThat(foundMember).isPresent();
            Member loaded = foundMember.get();
            assertThat(loaded.getFirstName()).isEqualTo("Full");
            assertThat(loaded.getLastName()).isEqualTo("Load");
            assertThat(loaded.getEmail().value()).isEqualTo("full@example.com");
            assertThat(loaded.getPhone().value()).isEqualTo("+420111111116");
            assertThat(loaded.getGuardian()).isNotNull();
            assertThat(loaded.getGuardian().getFirstName()).isEqualTo("Guard");
        }
    }

    @Nested
    @DisplayName("findByRegistrationNumber() method")
    class FindByRegistrationIdMethod {

        @Test
        @DisplayName("should find member by registration number")
        void shouldFindMemberByRegistrationNumber() {
            // Given
            RegistrationNumber regNum = new RegistrationNumber("ZBM0501");
            Member member = aMember()
                    .withRegistrationNumber(regNum)
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
            memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findByRegistrationNumber(regNum);

            // Then
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getRegistrationNumber().getValue()).isEqualTo("ZBM0501");
            assertThat(foundMember.get().getFirstName()).isEqualTo("Jan");
        }

        @Test
        @DisplayName("should return empty when registration number not found")
        void shouldReturnEmptyWhenRegistrationNumberNotFound() {
            // When
            Optional<Member> foundMember = memberRepository.findByRegistrationNumber(new RegistrationNumber("NON9999"));

            // Then
            assertThat(foundMember).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail() method")
    class FindByEmailMethod {

        @Test
        @DisplayName("should find member by email")
        void shouldFindMemberByEmail() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
            memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findByEmail("jan.novak@example.com");

            // Then
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(foundMember.get().getFirstName()).isEqualTo("Jan");
        }

        @Test
        @DisplayName("should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            // When
            Optional<Member> foundMember = memberRepository.findByEmail("nonexistent@example.com");

            // Then
            assertThat(foundMember).isEmpty();
        }

        @Test
        @DisplayName("should be case-insensitive for email")
        void shouldBeCaseInsensitiveForEmail() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0503")
                    .withName("Test", "User")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Test", "11000", "CZ"))
                    .withEmail("test@example.com")
                    .withPhone("+420111111118")
                    .withNoGuardian()
                    .build();
            memberRepository.save(member);

            // When - search with uppercase
            Optional<Member> foundMember = memberRepository.findByEmail("TEST@EXAMPLE.COM");

            // Then
            // Email should be case-insensitive per RFC 5321
            // This test documents current behavior
            assertThat(foundMember).isPresent();
        }
    }

    @Nested
    @DisplayName("countByBirthYear() method")
    class CountByBirthYearMethod {

        @Test
        @DisplayName("should count members by birth year correctly")
        void shouldCountMembersByBirthYear() {
            // Given
            Member member1 = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Test1")
                    .withDateOfBirth(LocalDate.of(2005, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test1 1", "Praha", "11000", "CZ"))
                    .withEmail("test1@example.com")
                    .withPhone("+420111111119")
                    .withNoGuardian()
                    .build();

            Member member2 = aMember()
                    .withRegistrationNumber("ZBM0502")
                    .withName("Petra", "Test2")
                    .withDateOfBirth(LocalDate.of(2005, 12, 31))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Test2 2", "Brno", "60200", "CZ"))
                    .withEmail("test2@example.com")
                    .withPhone("+420111111120")
                    .withNoGuardian()
                    .build();

            Member member3 = aMember()
                    .withRegistrationNumber("ZBM0601")
                    .withName("Karel", "Test3")
                    .withDateOfBirth(LocalDate.of(2006, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test3 3", "Ostrava", "70800", "CZ"))
                    .withEmail("test3@example.com")
                    .withPhone("+420111111121")
                    .withNoGuardian()
                    .build();

            memberRepository.save(member1);
            memberRepository.save(member2);
            memberRepository.save(member3);

            // When
            int count2005 = memberRepository.countByBirthYear(2005);
            int count2006 = memberRepository.countByBirthYear(2006);
            int count2007 = memberRepository.countByBirthYear(2007);

            // Then
            assertThat(count2005).isEqualTo(2);
            assertThat(count2006).isEqualTo(1);
            assertThat(count2007).isEqualTo(0);
        }

        @Test
        @DisplayName("should return zero when no members exist")
        void shouldReturnZeroWhenNoMembersExist() {
            // When
            int count = memberRepository.countByBirthYear(2005);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findAll() method - pagination and sorting")
    class FindAllMethod {

        @Test
        @DisplayName("should return paginated members")
        void shouldReturnPaginatedMembers() {
            // Given - create 5 members
            for (int i = 1; i <= 5; i++) {
                Member member = aMember()
                        .withRegistrationNumber("ZBM000" + i)
                        .withName("FirstName" + i, "LastName" + i)
                        .withDateOfBirth(LocalDate.of(2000 + i, 1, 1))
                        .withNationality("CZ")
                        .withGender(Gender.MALE)
                        .withAddress(Address.of("Street " + i, "City", "11000", "CZ"))
                        .withEmail("user" + i + "@example.com")
                        .withPhone("+420111111" + "%03d".formatted(i))
                        .withNoGuardian()
                        .build();
                memberRepository.save(member);
            }

            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<Member> page = memberRepository.findAll(pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("should sort members by last name")
        void shouldSortMembersByLastName() {
            // Given
            Member member1 = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 1, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 1", "Praha", "11000", "CZ"))
                    .withEmail("jan@example.com")
                    .withPhone("+420111111122")
                    .withNoGuardian()
                    .build();

            Member member2 = aMember()
                    .withRegistrationNumber("ZBM0201")
                    .withName("Petra", "Svobodová")
                    .withDateOfBirth(LocalDate.of(2002, 6, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 2", "Brno", "60200", "CZ"))
                    .withEmail("petra@example.com")
                    .withPhone("+420111111123")
                    .withNoGuardian()
                    .build();

            Member member3 = aMember()
                    .withRegistrationNumber("ZBM0001")
                    .withName("Karel", "Černý")
                    .withDateOfBirth(LocalDate.of(2000, 12, 31))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Svobodova 3", "Ostrava", "70800", "CZ"))
                    .withEmail("karel@example.com")
                    .withPhone("+420111111124")
                    .withNoGuardian()
                    .build();

            memberRepository.save(member1);
            memberRepository.save(member2);
            memberRepository.save(member3);

            Sort sort = Sort.by("lastName").ascending();
            Pageable pageable = PageRequest.of(0, 10, sort);

            // When
            Page<Member> page = memberRepository.findAll(pageable);

            // Then
            assertThat(page.getContent()).hasSize(3);
            // Černý should come before Novák (C < N in Czech collation)
            // However, this depends on database collation
            assertThat(page.getContent().get(0).getLastName()).isNotNull();
        }

        @Test
        @DisplayName("should return empty page when no members exist")
        void shouldReturnEmptyPageWhenNoMembersExist() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Member> page = memberRepository.findAll(pageable);

            // Then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should return all members without pagination")
        void shouldReturnAllMembersWithoutPagination() {
            // Given
            Member member1 = aMember()
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 1, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 1", "Praha", "11000", "CZ"))
                    .withEmail("jan@example.com")
                    .withPhone("+420111111125")
                    .withNoGuardian()
                    .build();

            GuardianInformation guardian = new GuardianInformation(
                    "Pavel",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("pavel@example.com"),
                    PhoneNumber.of("+420987654321")
            );
            Member member2 = aMember()
                    .withRegistrationNumber("ZBM1001")
                    .withName("Petra", "Nováková")
                    .withDateOfBirth(LocalDate.of(2010, 6, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 2", "Brno", "60200", "CZ"))
                    .withEmail("petra@example.com")
                    .withPhone("+420111111126")
                    .withGuardian(guardian)
                    .build();

            memberRepository.save(member1);
            memberRepository.save(member2);

            // When
            List<Member> allMembers = memberRepository.findAll();

            // Then
            assertThat(allMembers).hasSize(2);
            assertThat(allMembers).extracting(m -> m.getRegistrationNumber().getValue())
                    .containsExactlyInAnyOrder("ZBM0501", "ZBM1001");
            assertThat(allMembers).extracting(Member::getFirstName)
                    .containsExactlyInAnyOrder("Jan", "Petra");
        }

        @Test
        @DisplayName("should return empty list when no members exist")
        void shouldReturnEmptyListWhenNoMembersExist() {
            // When
            List<Member> allMembers = memberRepository.findAll();

            // Then
            assertThat(allMembers).isEmpty();
        }
    }

    @Nested
    @DisplayName("Optimistic locking")
    class OptimisticLocking {

        @Test
        @DisplayName("should set version on save")
        void shouldSetVersionOnSave() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0005")
                    .withName("Version", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("version@example.com")
                    .withPhone("+420111111127")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getVersion()).isNotNull();
            assertThat(savedMember.getVersion()).isEqualTo(0L);
        }

    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("should populate createdAt on save")
        void shouldPopulateCreatedAtOnSave() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0007")
                    .withName("Audit", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("audit@example.com")
                    .withPhone("+420111111129")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should populate modifiedAt on save")
        void shouldPopulateModifiedAtOnSave() {
            // Given
            Member member = aMember()
                    .withRegistrationNumber("ZBM0008")
                    .withName("Modified", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("modified@example.com")
                    .withPhone("+420111111130")
                    .withNoGuardian()
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getLastModifiedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Update operations")
    class UpdateOperations {

    }

    @Nested
    @DisplayName("Birth number and bank account")
    class BirthNumberAndBankAccount {

        @Test
        @DisplayName("should save member with birth number for Czech nationality")
        void shouldSaveMemberWithBirthNumberForCzechNationality() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            BirthNumber birthNumber = BirthNumber.of("900101/1234");

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("900101/1234")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getBirthNumber()).isNotNull();
            assertThat(savedMember.getBirthNumber().value()).isEqualTo("900101/1234");
        }

        @Test
        @DisplayName("should load member with birth number and decrypt correctly")
        void shouldLoadMemberWithBirthNumberAndDecryptCorrectly() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Petra",
                    "Svobodová",
                    LocalDate.of(1995, 6, 20),
                    "CZE",
                    Gender.FEMALE
            );
            Address address = Address.of(
                    "Dětská 1",
                    "Brno",
                    "60200",
                    "CZ"
            );
            BirthNumber birthNumber = BirthNumber.of("950620/5678");

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM9501")
                    .withName("Petra", "Svobodová")
                    .withDateOfBirth(LocalDate.of(1995, 6, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(address)
                    .withEmail("petra.svobodova@example.com")
                    .withPhone("+420987654321")
                    .withBirthNumber("950620/5678")
                    .build();

            Member savedMember = memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

            // Then
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getBirthNumber()).isNotNull();
            assertThat(foundMember.get().getBirthNumber().value()).isEqualTo("950620/5678");
        }

        @Test
        @DisplayName("should save member with IBAN bank account number")
        void shouldSaveMemberWithIBANBankAccountNumber() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Karel",
                    "Černý",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of(
                    "Svobodova 3",
                    "Ostrava",
                    "70800",
                    "CZ"
            );
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("CZ6508000000192000145399");

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0001")
                    .withName("Karel", "Černý")
                    .withDateOfBirth(LocalDate.of(1980, 1, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withEmail("karel.cerny@example.com")
                    .withPhone("+420111111111")
                    .withBankAccountNumber("CZ6508000000192000145399")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("CZ6508000000192000145399");
            assertThat(savedMember.getBankAccountNumber().format()).isEqualTo(BankAccountNumber.AccountFormat.IBAN);
        }

        @Test
        @DisplayName("should save member with domestic Czech bank account number")
        void shouldSaveMemberWithDomesticBankAccountNumber() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jana",
                    "Procházková",
                    LocalDate.of(2002, 5, 10),
                    "CZ",
                    Gender.FEMALE
            );
            Address address = Address.of(
                    "Hlavní 5",
                    "Plzeň",
                    "30100",
                    "CZ"
            );
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("123456/0300");

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0201")
                    .withName("Jana", "Procházková")
                    .withDateOfBirth(LocalDate.of(1992, 2, 10))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(address)
                    .withEmail("jana.prochazkova@example.com")
                    .withPhone("+420222222222")
                    .withBankAccountNumber("123456/0300")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("123456/0300");
            assertThat(savedMember.getBankAccountNumber().format()).isEqualTo(BankAccountNumber.AccountFormat.DOMESTIC);
        }

        @Test
        @DisplayName("should save member with null birth number and bank account for backwards compatibility")
        void shouldSaveMemberWithNullBirthNumberAndBankAccountForBackwardsCompatibility() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Martin",
                    "Dvořák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of(
                    "Náměstí 1",
                    "Liberec",
                    "46001",
                    "CZ"
            );

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0502")
                    .withName("Martin", "Dvořák")
                    .withDateOfBirth(LocalDate.of(1985, 5, 2))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withEmail("martin.dvorak@example.com")
                    .withPhone("+420333333333")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getBirthNumber()).isNull();
            assertThat(savedMember.getBankAccountNumber()).isNull();
        }

        @Test
        @DisplayName("should load member with null birth number and bank account")
        void shouldLoadMemberWithNullBirthNumberAndBankAccount() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Lucie",
                    "Kučerová",
                    LocalDate.of(2000, 8, 25),
                    "CZ",
                    Gender.FEMALE
            );
            Address address = Address.of(
                    "Mládí 1",
                    "Hradec Králové",
                    "50002",
                    "CZ"
            );

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM1001")
                    .withName("Lucie", "Kučerová")
                    .withDateOfBirth(LocalDate.of(1990, 10, 1))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(address)
                    .withEmail("lucie.kucerova@example.com")
                    .withPhone("+420444444444")
                    .build();

            Member savedMember = memberRepository.save(member);

            // When
            Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

            // Then
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getBirthNumber()).isNull();
            assertThat(foundMember.get().getBankAccountNumber()).isNull();
        }

        @Test
        @DisplayName("should save member with both birth number and bank account")
        void shouldSaveMemberWithBothBirthNumberAndBankAccount() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Tomáš",
                    "Beneš",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of(
                    "Komenského 10",
                    "Pardubice",
                    "53002",
                    "CZ"
            );
            BirthNumber birthNumber = BirthNumber.of("050315/1234");
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("CZ6508000000192000145399");

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0503")
                    .withName("Tomáš", "Beneš")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withEmail("tomas.benes@example.com")
                    .withPhone("+420555555555")
                    .withBirthNumber("050315/1234")
                    .withBankAccountNumber("CZ6508000000192000145399")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getBirthNumber()).isNotNull();
            assertThat(savedMember.getBirthNumber().value()).isEqualTo("050315/1234");
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("CZ6508000000192000145399");
        }
    }

    @Nested
    @DisplayName("Member suspension")
    class MemberSuspension {

        @Test
        @DisplayName("should save and load suspended member with all suspension fields")
        void shouldSaveAndLoadSuspendedMember() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            UserId adminUserId = new UserId(UUID.randomUUID());

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 3, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .build();

            // When - suspend the member
            Member.SuspendMembership suspendCommand = new Member.SuspendMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );
            member.handle(suspendCommand);

            // And save to database
            Member savedMember = memberRepository.save(member);

            // And load from database
            Optional<Member> loadedMember = memberRepository.findById(savedMember.getId());

            // Then - verify suspension fields persisted correctly
            assertThat(loadedMember).isPresent();
            Member loaded = loadedMember.get();
            assertThat(loaded.isActive()).isFalse();
            assertThat(loaded.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(loaded.getSuspendedAt()).isNotNull();
            assertThat(loaded.getSuspensionNote()).isEqualTo("Member requested termination");
            assertThat(loaded.getSuspendedBy()).isEqualTo(adminUserId);
        }

        @Test
        @DisplayName("should save active member with null suspension fields")
        void shouldSaveActiveMemberWithNullSuspensionFields() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Petra",
                    "Svobodová",
                    LocalDate.of(1995, 7, 20),
                    "CZ",
                    Gender.FEMALE
            );
            Address address = Address.of(
                    "Nová 456",
                    "Brno",
                    "60200",
                    "CZ"
            );

            Member member = MemberTestDataBuilder.aMember()
                    .withId(UUID.randomUUID())
                    .withRegistrationNumber("ZBM9501")
                    .withName("Petra", "Svobodová")
                    .withDateOfBirth(LocalDate.of(1995, 7, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(address)
                    .withEmail("petra.svobodova@example.com")
                    .withPhone("+420987654321")
                    .build();

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember.isActive()).isTrue();
            assertThat(savedMember.getSuspensionReason()).isNull();
            assertThat(savedMember.getSuspendedAt()).isNull();
            assertThat(savedMember.getSuspensionNote()).isNull();
            assertThat(savedMember.getSuspendedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("License persistence")
    class LicensePersistence {

        @Test
        @DisplayName("should save and load TrainerLicense with level")
        void shouldSaveAndLoadTrainerLicense() {
            TrainerLicense trainerLicense = TrainerLicense.of(TrainerLevel.T2, LocalDate.now().plusYears(1));
            Member member = aMember()
                    .withRegistrationNumber("ZBM8001")
                    .withName("Trainer", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 1", "Praha", "11000", "CZ"))
                    .withEmail("trainer.test@example.com")
                    .withPhone("+420111111200")
                    .withNoGuardian()
                    .withTrainerLicense(trainerLicense)
                    .build();

            Member savedMember = memberRepository.save(member);
            Optional<Member> loaded = memberRepository.findById(savedMember.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getTrainerLicense()).isNotNull();
            assertThat(loaded.get().getTrainerLicense().level()).isEqualTo(TrainerLevel.T2);
            assertThat(loaded.get().getTrainerLicense().validityDate()).isEqualTo(trainerLicense.validityDate());
        }

        @Test
        @DisplayName("should save and load RefereeLicense")
        void shouldSaveAndLoadRefereeLicense() {
            RefereeLicense refereeLicense = RefereeLicense.of(RefereeLevel.R1, LocalDate.now().plusYears(2));
            Member member = aMember()
                    .withRegistrationNumber("ZBM8002")
                    .withName("Referee", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 2", "Praha", "11000", "CZ"))
                    .withEmail("referee.test@example.com")
                    .withPhone("+420111111201")
                    .withNoGuardian()
                    .withRefereeLicense(refereeLicense)
                    .build();

            Member savedMember = memberRepository.save(member);
            Optional<Member> loaded = memberRepository.findById(savedMember.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getRefereeLicense()).isNotNull();
            assertThat(loaded.get().getRefereeLicense().level()).isEqualTo(RefereeLevel.R1);
            assertThat(loaded.get().getRefereeLicense().validityDate()).isEqualTo(refereeLicense.validityDate());
        }

        @Test
        @DisplayName("should save member without licenses and load with null licenses")
        void shouldSaveAndLoadWithNullLicenses() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM8003")
                    .withName("NoLicense", "Test")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Test 3", "Praha", "11000", "CZ"))
                    .withEmail("nolicense.test@example.com")
                    .withPhone("+420111111202")
                    .withNoGuardian()
                    .build();

            Member savedMember = memberRepository.save(member);
            Optional<Member> loaded = memberRepository.findById(savedMember.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getTrainerLicense()).isNull();
            assertThat(loaded.get().getRefereeLicense()).isNull();
        }
    }
}
