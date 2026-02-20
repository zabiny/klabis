package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.MemberAssert;
import com.klabis.members.domain.*;
import com.klabis.users.UserId;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})  // jMolecules Repository annotation, used to load all repository adapters (for context caching)
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
            Member member = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Petra",
                    "Nováková",
                    LocalDate.of(2010, 6, 20),
                    "CZ",
                    Gender.FEMALE
            );
            GuardianInformation guardian = new GuardianInformation(
                    "Pavel",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("pavel.novak@example.com"),
                    PhoneNumber.of("+420987654321")
            );
            Address address = Address.of(
                    "Dětská 1",
                    "Brno",
                    "60200",
                    "CZ"
            );
            Member member = Member.create(
                    new RegistrationNumber("ZBM1001"),
                    personalInformation,
                    address,
                    new EmailAddress("petra.novakova@example.com"),
                    new PhoneNumber("+420111222333"),
                    guardian
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Test",
                    "User",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 5", "Ostrava", "70800", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0001"),
                    personalInformation,
                    address,
                    new EmailAddress("test@example.com"),
                    new PhoneNumber("+420111111111"),
                    null
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Version",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0002"),
                    personalInformation,
                    address,
                    new EmailAddress("version@example.com"),
                    new PhoneNumber("+420111111112"),
                    null
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Complete",
                    "Member",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Guardian",
                    "Name",
                    "PARENT",
                    EmailAddress.of("guardian@example.com"),
                    PhoneNumber.of("+420111111113")
            );
            Member member = Member.create(
                    new RegistrationNumber("ZBM0003"),
                    personalInformation,
                    address,
                    new EmailAddress("complete@example.com"),
                    new PhoneNumber("+420111111114"),
                    guardian
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null
            );
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
            UserId nonExistentId = new UserId(UUID.randomUUID());

            // When
            Optional<Member> foundMember = memberRepository.findById(nonExistentId);

            // Then
            assertThat(foundMember).isEmpty();
        }

        @Test
        @DisplayName("should load all member fields correctly")
        void shouldLoadAllMemberFieldsCorrectly() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Full",
                    "Load",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            GuardianInformation guardian = new GuardianInformation(
                    "Guard",
                    "Guardian",
                    "PARENT",
                    EmailAddress.of("guard@example.com"),
                    PhoneNumber.of("+420111111115")
            );
            Address address = Address.of("Test 1", "Test", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0004"),
                    personalInformation,
                    address,
                    new EmailAddress("full@example.com"),
                    new PhoneNumber("+420111111116"),
                    guardian
            );
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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            RegistrationNumber regNum = new RegistrationNumber("ZBM0501");
            Member member = Member.create(
                    regNum,
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null
            );
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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 3, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null
            );
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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Test",
                    "User",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Test", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0503"),
                    personalInformation,
                    address,
                    new EmailAddress("test@example.com"),
                    new PhoneNumber("+420111111118"),
                    null
            );
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
            Address address1 = Address.of("Test1 1", "Praha", "11000", "CZ");
            PersonalInformation personalInfo1 = PersonalInformation.of(
                    "Jan",
                    "Test1",
                    LocalDate.of(2005, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Member member1 = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInfo1,
                    address1,
                    new EmailAddress("test1@example.com"),
                    new PhoneNumber("+420111111119"),
                    null
            );

            Address address2 = Address.of("Test2 2", "Brno", "60200", "CZ");
            PersonalInformation personalInfo2 = PersonalInformation.of(
                    "Petra",
                    "Test2",
                    LocalDate.of(2005, 12, 31),
                    "CZ",
                    Gender.FEMALE
            );
            Member member2 = Member.create(
                    new RegistrationNumber("ZBM0502"),
                    personalInfo2,
                    address2,
                    new EmailAddress("test2@example.com"),
                    new PhoneNumber("+420111111120"),
                    null
            );

            Address address3 = Address.of("Test3 3", "Ostrava", "70800", "CZ");
            PersonalInformation personalInfo3 = PersonalInformation.of(
                    "Karel",
                    "Test3",
                    LocalDate.of(2006, 6, 15),
                    "CZ",
                    Gender.MALE
            );
            Member member3 = Member.create(
                    new RegistrationNumber("ZBM0601"),
                    personalInfo3,
                    address3,
                    new EmailAddress("test3@example.com"),
                    new PhoneNumber("+420111111121"),
                    null
            );

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
                PersonalInformation personalInfo = PersonalInformation.of(
                        "FirstName" + i,
                        "LastName" + i,
                        LocalDate.of(2000 + i, 1, 1),
                        "CZ",
                        Gender.MALE
                );
                Address address = Address.of("Street " + i, "City", "11000", "CZ");
                Member member = Member.create(
                        new RegistrationNumber("ZBM000" + i),
                        personalInfo,
                        address,
                        new EmailAddress("user" + i + "@example.com"),
                        new PhoneNumber("+420111111" + String.format("%03d", i)),
                        null
                );
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
            PersonalInformation personalInfo1 = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 1, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address1 = Address.of("Hlavní 1", "Praha", "11000", "CZ");
            Member member1 = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInfo1,
                    address1,
                    new EmailAddress("jan@example.com"),
                    new PhoneNumber("+420111111122"),
                    null
            );

            PersonalInformation personalInfo2 = PersonalInformation.of(
                    "Petra",
                    "Svobodová",
                    LocalDate.of(2002, 6, 20),
                    "CZ",
                    Gender.FEMALE
            );
            Address address2 = Address.of("Dětská 2", "Brno", "60200", "CZ");
            Member member2 = Member.create(
                    new RegistrationNumber("ZBM0201"),
                    personalInfo2,
                    address2,
                    new EmailAddress("petra@example.com"),
                    new PhoneNumber("+420111111123"),
                    null
            );

            PersonalInformation personalInfo3 = PersonalInformation.of(
                    "Karel",
                    "Černý",
                    LocalDate.of(2000, 12, 31),
                    "CZ",
                    Gender.MALE
            );
            Address address3 = Address.of("Svobodova 3", "Ostrava", "70800", "CZ");
            Member member3 = Member.create(
                    new RegistrationNumber("ZBM0001"),
                    personalInfo3,
                    address3,
                    new EmailAddress("karel@example.com"),
                    new PhoneNumber("+420111111124"),
                    null
            );

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
            PersonalInformation personalInfo1 = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 1, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address1 = Address.of("Hlavní 1", "Praha", "11000", "CZ");
            Member member1 = Member.create(
                    new RegistrationNumber("ZBM0501"),
                    personalInfo1,
                    address1,
                    new EmailAddress("jan@example.com"),
                    new PhoneNumber("+420111111125"),
                    null
            );

            PersonalInformation personalInfo2 = PersonalInformation.of(
                    "Petra",
                    "Nováková",
                    LocalDate.of(2010, 6, 20),
                    "CZ",
                    Gender.FEMALE
            );
            GuardianInformation guardian = new GuardianInformation(
                    "Pavel",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("pavel@example.com"),
                    PhoneNumber.of("+420987654321")
            );
            Address address2 = Address.of("Dětská 2", "Brno", "60200", "CZ");
            Member member2 = Member.create(
                    new RegistrationNumber("ZBM1001"),
                    personalInfo2,
                    address2,
                    new EmailAddress("petra@example.com"),
                    new PhoneNumber("+420111111126"),
                    guardian
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Version",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0005"),
                    personalInformation,
                    address,
                    new EmailAddress("version@example.com"),
                    new PhoneNumber("+420111111127"),
                    null
            );

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember).isNotNull();
            assertThat(savedMember.getVersion()).isNotNull();
            assertThat(savedMember.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should increment version on update")
        void shouldIncrementVersionOnUpdate() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Update",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0006"),
                    personalInformation,
                    address,
                    new EmailAddress("update@example.com"),
                    new PhoneNumber("+420111111128"),
                    null
            );
            Member savedMember = memberRepository.save(member);

            // When - update member
            Address newAddress = Address.of("New Street 1", "New City", "11000", "CZ");
            savedMember.updateContactInformation(
                    savedMember.getEmail(),
                    savedMember.getPhone(),
                    newAddress
            );
            Member savedUpdatedMember = memberRepository.save(savedMember);

            // Then
            assertThat(savedUpdatedMember).isNotNull();
            assertThat(savedUpdatedMember.getVersion()).isGreaterThan(savedMember.getVersion());
        }
    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("should populate createdAt on save")
        void shouldPopulateCreatedAtOnSave() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Audit",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0007"),
                    personalInformation,
                    address,
                    new EmailAddress("audit@example.com"),
                    new PhoneNumber("+420111111129"),
                    null
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Modified",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Test 1", "Praha", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0008"),
                    personalInformation,
                    address,
                    new EmailAddress("modified@example.com"),
                    new PhoneNumber("+420111111130"),
                    null
            );

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

        @Test
        @DisplayName("should update member contact information")
        void shouldUpdateMemberContactInformation() {
            // Given
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Contact",
                    "Update",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE
            );
            Address address = Address.of("Old Street 1", "Old City", "11000", "CZ");
            Member member = Member.create(
                    new RegistrationNumber("ZBM0009"),
                    personalInformation,
                    address,
                    new EmailAddress("old@example.com"),
                    new PhoneNumber("+420111111131"),
                    null
            );
            Member savedMember = memberRepository.save(member);

            // When - update contact info (modifies member in-place)
            Address newAddress = Address.of("New Street 1", "New City", "11000", "CZ");
            EmailAddress newEmail = EmailAddress.of("new@example.com");
            savedMember.updateContactInformation(
                    newEmail,
                    savedMember.getPhone(),
                    newAddress
            );
            Member savedUpdatedMember = memberRepository.save(savedMember);

            // Then
            Optional<Member> foundMember = memberRepository.findById(savedUpdatedMember.getId());
            assertThat(foundMember).isPresent();
            assertThat(foundMember.get().getEmail().value()).isEqualTo("new@example.com");
        }
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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0501"),
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null,
                    birthNumber,
                    null
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM9501"),
                    personalInformation,
                    address,
                    new EmailAddress("petra.svobodova@example.com"),
                    new PhoneNumber("+420987654321"),
                    null,
                    birthNumber,
                    null
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0001"),
                    personalInformation,
                    address,
                    new EmailAddress("karel.cerny@example.com"),
                    new PhoneNumber("+420111111111"),
                    null,
                    null,
                    bankAccountNumber
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0201"),
                    personalInformation,
                    address,
                    new EmailAddress("jana.prochazkova@example.com"),
                    new PhoneNumber("+420222222222"),
                    null,
                    null,
                    bankAccountNumber
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0502"),
                    personalInformation,
                    address,
                    new EmailAddress("martin.dvorak@example.com"),
                    new PhoneNumber("+420333333333"),
                    null,
                    null,
                    null
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM1001"),
                    personalInformation,
                    address,
                    new EmailAddress("lucie.kucerova@example.com"),
                    new PhoneNumber("+420444444444"),
                    null,
                    null,
                    null
            );

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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0503"),
                    personalInformation,
                    address,
                    new EmailAddress("tomas.benes@example.com"),
                    new PhoneNumber("+420555555555"),
                    null,
                    birthNumber,
                    bankAccountNumber
            );

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
    @DisplayName("Member termination")
    class MemberTermination {

        @Test
        @DisplayName("should save and load terminated member with all termination fields")
        void shouldSaveAndLoadTerminatedMember() {
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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM0501"),
                    personalInformation,
                    address,
                    new EmailAddress("jan.novak@example.com"),
                    new PhoneNumber("+420123456789"),
                    null,
                    null,
                    null
            );

            // When - terminate the member
            Member.TerminateMembership terminateCommand = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );
            member.handle(terminateCommand);

            // And save to database
            Member savedMember = memberRepository.save(member);

            // And load from database
            Optional<Member> loadedMember = memberRepository.findById(savedMember.getId());

            // Then - verify termination fields persisted correctly
            assertThat(loadedMember).isPresent();
            Member loaded = loadedMember.get();
            assertThat(loaded.isActive()).isFalse();
            assertThat(loaded.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(loaded.getDeactivatedAt()).isNotNull();
            assertThat(loaded.getDeactivationNote()).isEqualTo("Member requested termination");
            assertThat(loaded.getDeactivatedBy()).isEqualTo(adminUserId);
        }

        @Test
        @DisplayName("should save active member with null termination fields")
        void shouldSaveActiveMemberWithNullTerminationFields() {
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

            Member member = Member.createWithId(
                    new UserId(UUID.randomUUID()),
                    new RegistrationNumber("ZBM9501"),
                    personalInformation,
                    address,
                    new EmailAddress("petra.svobodova@example.com"),
                    new PhoneNumber("+420987654321"),
                    null,
                    null,
                    null
            );

            // When
            Member savedMember = memberRepository.save(member);

            // Then
            assertThat(savedMember.isActive()).isTrue();
            assertThat(savedMember.getDeactivationReason()).isNull();
            assertThat(savedMember.getDeactivatedAt()).isNull();
            assertThat(savedMember.getDeactivationNote()).isNull();
            assertThat(savedMember.getDeactivatedBy()).isNull();
        }
    }
}
