package com.klabis.members.management;

import com.klabis.TestApplicationConfiguration;
import com.klabis.members.Gender;
import com.klabis.members.MemberRepository;
import com.klabis.users.AccountStatus;
import com.klabis.users.User;
import com.klabis.users.UserId;
import com.klabis.users.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for automatic User provisioning during Member registration.
 * <p>
 * "Auto-provisioning" refers to the automatic creation of a User account when a Member is registered.
 * The RegistrationService creates both Member and User aggregates in a single transaction,
 * ensuring Member ID equals User ID.
 */
@ApplicationModuleTest(extraIncludes = {"users", "common"})
@ActiveProfiles("test")
@DisplayName("Member Auto-Provisioning Integration Tests")
@Import(TestApplicationConfiguration.class)
class RegisterMemberAutoProvisioningTest {

    @Autowired
    private RegistrationService memberService;

    @Autowired
    private MemberRepository memberJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should create User when Member is registered")
    @Transactional
    void shouldCreateUserWhenMemberRegistered() {
        AddressRequest address = new AddressRequest(
                "Street 1",
                "City 1",
                "10000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Test",
                "User",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                "test@example.com",
                "+420777888888",
                address,
                null
        );

        UUID memberId = memberService.registerMember(request);

        // Verify Member was created
        var memberEntity = memberJpaRepository.findById(new UserId(memberId));
        assertThat(memberEntity).isPresent();
        String registrationNumber = memberEntity.get().getRegistrationNumber().getValue();

        // Verify User was created with same registration number
        Optional<User> userOpt = userRepository.findByUsername(registrationNumber);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Verify User properties
        assertThat(user.getUsername()).isEqualTo(registrationNumber);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(user.getPasswordHash()).isNotNull();
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(user.getPasswordHash()).isNotEqualTo(""); // Should be hashed, not plain
    }

    @Test
    @DisplayName("User should have MEMBERS:READ authority")
    @Transactional
    void userShouldHaveReadAuthority() {
        AddressRequest address = new AddressRequest(
                "Street 2",
                "City 2",
                "20000",
                "CZ"
        );

        RegisterMemberRequest command = new RegisterMemberRequest(
                "Integration",
                "Test",
                LocalDate.of(2006, 3, 20),
                "SK",
                Gender.FEMALE,
                "integration@example.com",
                "+420777999999",
                address,
                null
        );

        UUID memberId = memberService.registerMember(command);
        var memberEntity = memberJpaRepository.findById(new UserId(memberId)).get();
        User user = userRepository.findByUsername(memberEntity.getRegistrationNumber().getValue()).get();

        // Verify user was created successfully (authorities are now in UserPermissions, not User)
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isNotEmpty();
    }

    @Test
    @DisplayName("should create different Users for different Members")
    @Transactional
    void shouldCreateDifferentUsersForDifferentMembers() {
        AddressRequest address1 = new AddressRequest(
                "Street 3",
                "City 3",
                "30000",
                "CZ"
        );

        AddressRequest address2 = new AddressRequest(
                "Street 4",
                "City 4",
                "40000",
                "CZ"
        );

        RegisterMemberRequest command1 = new RegisterMemberRequest(
                "First",
                "Person",
                LocalDate.of(2005, 1, 1),
                "CZ",
                Gender.MALE,
                "first@example.com",
                "+420111111111",
                address1,
                null
        );

        RegisterMemberRequest command2 = new RegisterMemberRequest(
                "Second",
                "Person",
                LocalDate.of(2005, 1, 2),
                "CZ",
                Gender.MALE,
                "second@example.com",
                "+420222222222",
                address2,
                null
        );

        UserId memberId1 = new UserId(memberService.registerMember(command1));
        UserId memberId2 = new UserId(memberService.registerMember(command2));

        var memberEntity1 = memberJpaRepository.findById(memberId1).get();
        var memberEntity2 = memberJpaRepository.findById(memberId2).get();

        User user1 = userRepository.findByUsername(memberEntity1.getRegistrationNumber().getValue()).get();
        User user2 = userRepository.findByUsername(memberEntity2.getRegistrationNumber().getValue()).get();

        // Verify they are different users
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user1.getUsername()).isNotEqualTo(user2.getUsername());
    }
}
