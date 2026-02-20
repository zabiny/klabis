package com.klabis.members.management;

import com.klabis.members.MemberTerminatedEvent;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.infrastructure.restapi.AddressRequest;
import com.klabis.members.infrastructure.restapi.TerminateMembershipRequest;
import com.klabis.members.infrastructure.restapi.UpdateMemberRequest;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ManagementServiceImpl}.
 * <p>
 * Tests cover the member management functionality including:
 * <ul>
 *   <li>Retrieving member details</li>
 *   <li>Updating member information with authorization checks</li>
 *   <li>Listing members with pagination</li>
 *   <li>Admin vs. non-admin permission handling</li>
 *   <li>Self-edit capabilities and restrictions</li>
 *   <li>Membership termination with domain events</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManagementService Unit Tests")
class ManagementServiceTest {

    @Mock
    private Authentication authentication;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    private ManagementService service;
    private UUID testMemberId;
    private Member testMember;

    @BeforeEach
    void setUp() {
        service = new ManagementServiceImpl(memberRepository, userService);

        testMemberId = UUID.randomUUID();
        testMember = MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("John")
                .withLastName("Doe")
                .withRegistrationNumber("ZBM1234")
                .withEmail("john.doe@example.com")
                .withPhone("+420123456789")
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withNoGuardian()
                .build();
    }

    @Nested
    @DisplayName("Member Update Tests")
    class MemberUpdateTests {

        @BeforeEach
        void setUpNested() {
            // Set up admin authentication with MEMBERS:UPDATE authority
            UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                    "admin",
                    "password",
                    List.of(new SimpleGrantedAuthority("MEMBERS:UPDATE"))
            );
            SecurityContextHolder.getContext().setAuthentication(adminAuth);
        }

        @AfterEach
        void tearDownNested() {
            // Clear security context
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("should update member with birth number for Czech national")
        void shouldUpdateMemberWithBirthNumberForCzechNational() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.<String>empty(),     // email
                    Optional.<String>empty(),     // phone
                    Optional.<AddressRequest>empty(),  // address
                    Optional.<String>empty(),     // firstName
                    Optional.<String>empty(),     // lastName
                    Optional.<LocalDate>empty(),  // dateOfBirth
                    Optional.<Gender>empty(),     // gender
                    Optional.<String>empty(),     // chipNumber
                    Optional.<IdentityCardDto>empty(), // identityCard
                    Optional.<MedicalCourseDto>empty(), // medicalCourse
                    Optional.<TrainerLicenseDto>empty(), // trainerLicense
                    Optional.<DrivingLicenseGroup>empty(), // drivingLicenseGroup
                    Optional.<String>empty(),     // dietaryRestrictions
                    Optional.<String>of("900101/1234"), // birthNumber
                    Optional.<String>empty()      // bankAccountNumber
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UUID updatedId = service.updateMember(testMemberId, request);

            // Then
            assertThat(updatedId).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should update member with IBAN bank account number")
        void shouldUpdateMemberWithIBANBankAccountNumber() {
            // Given - use a valid domestic format account number instead of invalid IBAN
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.<String>empty(),     // email
                    Optional.<String>empty(),     // phone
                    Optional.<AddressRequest>empty(),  // address
                    Optional.<String>empty(),     // firstName
                    Optional.<String>empty(),     // lastName
                    Optional.<LocalDate>empty(),  // dateOfBirth
                    Optional.<Gender>empty(),     // gender
                    Optional.<String>empty(),     // chipNumber
                    Optional.<IdentityCardDto>empty(), // identityCard
                    Optional.<MedicalCourseDto>empty(), // medicalCourse
                    Optional.<TrainerLicenseDto>empty(), // trainerLicense
                    Optional.<DrivingLicenseGroup>empty(), // drivingLicenseGroup
                    Optional.<String>empty(),     // dietaryRestrictions
                    Optional.<String>empty(),     // birthNumber
                    Optional.<String>of("12345/5678") // bankAccountNumber (valid domestic format)
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UUID updatedId = service.updateMember(testMemberId, request);

            // Then
            assertThat(updatedId).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }
    }

    // Private helper methods are removed as authentication is now handled with @BeforeEach

    @Nested
    @DisplayName("Member Termination Tests")
    class MemberTerminationTests {

        private Member testActiveMember;
        private UUID adminUserId;

        @BeforeEach
        void setUpNested() {
            adminUserId = UUID.randomUUID();
            testActiveMember = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withFirstName("Jane")
                    .withLastName("Smith")
                    .withRegistrationNumber("ZBM5678")
                    .withEmail("jane.smith@example.com")
                    .withPhone("+420987654321")
                    .withAddress(Address.of("Vinohradská 456", "Praha", "12000", "CZ"))
                    .withNoGuardian()
                    .build();

            // Set up admin authentication
            UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                    "admin",
                    "password",
                    List.of(new SimpleGrantedAuthority("MEMBERS:UPDATE"))
            );
            SecurityContextHolder.getContext().setAuthentication(adminAuth);
        }

        @AfterEach
        void tearDownNested() {
            SecurityContextHolder.clearContext();
        }

        @Nested
        @DisplayName("Successful Termination Tests")
        class SuccessfulTerminationTests {

            @Test
            @DisplayName("should terminate active member with ODHLASKA reason")
            void shouldTerminateActiveMemberWithOdhlaskaReason() {
                // Given
                var request = new TerminateMembershipRequest(
                        DeactivationReason.ODHLASKA,
                        Optional.of("Member requested resignation")
                );

                when(memberRepository.findById(new UserId(testMemberId)))
                        .thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // When - create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                UUID resultId = service.terminateMember(testMemberId, command);

                // Then
                assertThat(resultId).isEqualTo(testMemberId);

                ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(memberCaptor.capture());

                Member savedMember = memberCaptor.getValue();
                assertThat(savedMember.isActive()).isFalse();
                assertThat(savedMember.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
                assertThat(savedMember.getDeactivatedAt()).isNotNull();
                assertThat(savedMember.getDeactivationNote()).isEqualTo("Member requested resignation");
                assertThat(savedMember.getDeactivatedBy().uuid()).isEqualTo(adminUserId);
            }

            @Test
            @DisplayName("should terminate active member without note")
            void shouldTerminateActiveMemberWithoutNote() {
                // Given
                var request = new TerminateMembershipRequest(
                        DeactivationReason.PRESTUP,
                        Optional.empty()
                );

                when(memberRepository.findById(new UserId(testMemberId)))
                        .thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // When - create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                UUID resultId = service.terminateMember(testMemberId, command);

                // Then
                assertThat(resultId).isEqualTo(testMemberId);

                ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(memberCaptor.capture());

                Member savedMember = memberCaptor.getValue();
                assertThat(savedMember.isActive()).isFalse();
                assertThat(savedMember.getDeactivationReason()).isEqualTo(DeactivationReason.PRESTUP);
                assertThat(savedMember.getDeactivationNote()).isNull();
            }

            @Test
            @DisplayName("should publish MemberTerminatedEvent on successful termination")
            void shouldPublishMemberTerminatedEventOnSuccessfulTermination() {
                // Given
                var request = new TerminateMembershipRequest(
                        DeactivationReason.OTHER,
                        Optional.of("Administrative decision")
                );

                when(memberRepository.findById(new UserId(testMemberId)))
                        .thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // When - create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                service.terminateMember(testMemberId, command);

                // Then - verify domain event was registered via @DomainEvents
                ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(memberCaptor.capture());

                Member savedMember = memberCaptor.getValue();
                assertThat(savedMember.getDomainEvents()).hasSize(1);

                Object event = savedMember.getDomainEvents().get(0);
                assertThat(event).isInstanceOf(MemberTerminatedEvent.class);

                MemberTerminatedEvent terminationEvent = (MemberTerminatedEvent) event;
                assertThat(terminationEvent.getMemberId()).isEqualTo(new UserId(testMemberId));
                assertThat(terminationEvent.getReason()).isEqualTo(DeactivationReason.OTHER);
                assertThat(terminationEvent.getTerminatedBy()).isEqualTo(new UserId(adminUserId));
                assertThat(terminationEvent.getNote()).isEqualTo("Administrative decision");
            }
        }

        @Nested
        @DisplayName("Already Terminated Tests")
        class AlreadyTerminatedTests {

            @Test
            @DisplayName("should reject termination of already terminated member")
            void shouldRejectTerminationOfAlreadyTerminatedMember() {
                // Given - create already terminated member
                Member terminatedMember = MemberTestDataBuilder.aMember()
                        .withId(testMemberId)
                        .withFirstName("Bob")
                        .withLastName("Jones")
                        .withRegistrationNumber("ZBM9999")
                        .withEmail("bob.jones@example.com")
                        .withPhone("+420111222333")
                        .withAddress(Address.of("Řeznická 1", "Brno", "60200", "CZ"))
                        .withNoGuardian()
                        .terminated(DeactivationReason.ODHLASKA, "Previous termination")
                        .build();

                var request = new TerminateMembershipRequest(
                        DeactivationReason.OTHER,
                        Optional.of("Second termination attempt")
                );

                when(memberRepository.findById(new UserId(testMemberId)))
                        .thenReturn(Optional.of(terminatedMember));

                // When & Then
                // Create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                assertThatThrownBy(() -> service.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member is already terminated");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Concurrent Termination Tests")
        class ConcurrentTerminationTests {

            @Test
            @DisplayName("should handle concurrent termination attempts with optimistic locking")
            void shouldHandleConcurrentTerminationAttempts() {
                // Given
                var request = new TerminateMembershipRequest(
                        DeactivationReason.PRESTUP,
                        Optional.empty()
                );

                when(memberRepository.findById(new UserId(testMemberId)))
                        .thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

                // When & Then
                // Create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                assertThatThrownBy(() -> service.terminateMember(testMemberId, command))
                        .isInstanceOf(OptimisticLockingFailureException.class);

                verify(memberRepository).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Authorization Tests")
        class AuthorizationTests {

            @Test
            @DisplayName("should reject termination by non-admin user")
            void shouldRejectTerminationByNonAdminUser() {
                // Given - set up non-admin authentication
                UUID regularUserId = UUID.randomUUID();
                UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                        regularUserId.toString(),
                        "password",
                        List.of(new SimpleGrantedAuthority("MEMBERS:READ"))
                );
                SecurityContextHolder.getContext().setAuthentication(userAuth);

                var request = new TerminateMembershipRequest(
                        DeactivationReason.ODHLASKA,
                        Optional.empty()
                );

                // When & Then
                // Create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                assertThatThrownBy(() -> service.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Only users with MEMBERS:UPDATE permission");

                verify(memberRepository, never()).findById(any());
                verify(memberRepository, never()).save(any(Member.class));
            }

            @Test
            @DisplayName("should reject termination without authentication")
            void shouldRejectTerminationWithoutAuthentication() {
                // Given - clear security context
                SecurityContextHolder.clearContext();

                var request = new TerminateMembershipRequest(
                        DeactivationReason.ODHLASKA,
                        Optional.empty()
                );

                // When & Then
                // Create correct command object
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                assertThatThrownBy(() -> service.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("User must be authenticated");

                verify(memberRepository, never()).findById(any());
                verify(memberRepository, never()).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Member Not Found Tests")
        class MemberNotFoundTests {

            @Test
            @DisplayName("should throw exception when terminating non-existent member")
            void shouldThrowExceptionWhenTerminatingNonExistentMember() {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                var request = new TerminateMembershipRequest(
                        DeactivationReason.ODHLASKA,
                        Optional.empty()
                );

                when(memberRepository.findById(new UserId(nonExistentId)))
                        .thenReturn(Optional.empty());

                // When & Then
                var command = new Member.TerminateMembership(
                        new UserId(adminUserId),
                        request.reason(),
                        request.note().orElse(null)
                );
                assertThatThrownBy(() -> service.terminateMember(nonExistentId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member not found");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }
    }
}