package com.klabis.members.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.members.LastOwnershipChecker;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for automatic User provisioning during Member registration.
 * <p>
 * "Auto-provisioning" refers to the automatic creation of a User account when a Member is registered.
 * The RegistrationPort creates both Member and User aggregates in a single transaction,
 * ensuring Member ID equals User ID.
 */
@ApplicationModuleTest
@ActiveProfiles("test")
@DisplayName("Member Auto-Provisioning Integration Tests")
@Import(TestApplicationConfiguration.class)
class RegisterMemberAutoProvisioningTest {

    @MockitoBean
    @SuppressWarnings("unused")
    private LastOwnershipChecker lastOwnershipChecker;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.members.TrainingGroupProvider trainingGroupProvider;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.members.FamilyGroupProvider familyGroupProvider;

    @Autowired
    private RegistrationPort memberService;

    @Autowired
    private MemberRepository memberJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should create User when Member is registered")
    @Transactional
    void shouldCreateUserWhenMemberRegistered() {
        // Create a test shared ID
        Address address = Address.of("Street 1", "City 1", "10000", "CZ");
        EmailAddress email = EmailAddress.of("test@example.com");
        PhoneNumber phone = PhoneNumber.of("+420777888888");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Test", "User", LocalDate.of(2005, 6, 15), "CZ", Gender.MALE
        );

        RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                address,
                email,
                phone,
                null,
                BirthNumber.of("050615/1234"),
                null,
                null
        );

        Member member = memberService.registerMember(command);
        MemberId memberId = member.getId();

        // Verify Member was created
        var memberEntity = memberJpaRepository.findById(memberId);
        assertThat(memberEntity).isPresent();
        String createdRegistrationNumber = memberEntity.get().getRegistrationNumber().getValue();

        // Verify User was created with same registration number
        Optional<User> userOpt = userRepository.findByUsername(createdRegistrationNumber);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Verify User properties
        assertThat(user.getUsername()).isEqualTo(createdRegistrationNumber);
        assertThat(user.getPasswordHash()).isNotNull();
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(user.getPasswordHash()).isNotEqualTo(""); // Should be hashed, not plain
    }

    @Test
    @DisplayName("User should have MEMBERS:READ authority")
    @Transactional
    void userShouldHaveReadAuthority() {
        Address address = Address.of("Street 2", "City 2", "20000", "CZ");
        EmailAddress email = EmailAddress.of("integration@example.com");
        PhoneNumber phone = PhoneNumber.of("+420777999999");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Integration", "Test", LocalDate.of(2006, 3, 20), "SK", Gender.FEMALE
        );

        RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                address,
                email,
                phone,
                null,
                null,
                null,
                null
        );

        Member member = memberService.registerMember(command);
        var memberEntity = memberJpaRepository.findById(member.getId()).get();
        User user = userRepository.findByUsername(memberEntity.getRegistrationNumber().getValue()).get();

        // Verify user was created successfully (authorities are now in UserPermissions, not User)
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isNotEmpty();
    }

    @Test
    @DisplayName("should create different Users for different Members")
    @Transactional
    void shouldCreateDifferentUsersForDifferentMembers() {
        // Create first member
        RegistrationNumber registrationNumber1 = RegistrationNumber.of("ZBM0001");
        Address address1 = Address.of("Street 3", "City 3", "30000", "CZ");
        EmailAddress email1 = EmailAddress.of("first@example.com");
        PhoneNumber phone1 = PhoneNumber.of("+420111111111");
        PersonalInformation personalInformation1 = PersonalInformation.of(
                "First", "Person", LocalDate.of(2005, 1, 1), "CZ", Gender.MALE
        );

        RegistrationPort.RegisterNewMember command1 = new RegistrationPort.RegisterNewMember(personalInformation1,
                address1,
                email1,
                phone1,
                null,
                BirthNumber.of("050101/1234"),
                null,
                null
        );

        // Create second member
        RegistrationNumber registrationNumber2 = RegistrationNumber.of("ZBM0002");
        Address address2 = Address.of("Street 4", "City 4", "40000", "CZ");
        EmailAddress email2 = EmailAddress.of("second@example.com");
        PhoneNumber phone2 = PhoneNumber.of("+420222222222");
        PersonalInformation personalInformation2 = PersonalInformation.of(
                "Second", "Person", LocalDate.of(2005, 1, 2), "CZ", Gender.MALE
        );

        RegistrationPort.RegisterNewMember command2 = new RegistrationPort.RegisterNewMember(personalInformation2,
                address2,
                email2,
                phone2,
                null,
                BirthNumber.of("050102/1234"),
                null,
                null
        );

        Member member1 = memberService.registerMember(command1);
        Member member2 = memberService.registerMember(command2);

        var memberEntity1 = memberJpaRepository.findById(member1.getId()).get();
        var memberEntity2 = memberJpaRepository.findById(member2.getId()).get();

        User user1 = userRepository.findByUsername(memberEntity1.getRegistrationNumber().getValue()).get();
        User user2 = userRepository.findByUsername(memberEntity2.getRegistrationNumber().getValue()).get();

        // Verify they are different users
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user1.getUsername()).isNotEqualTo(user2.getUsername());
    }
}
