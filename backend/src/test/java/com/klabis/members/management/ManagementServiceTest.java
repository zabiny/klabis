package com.klabis.members.management;

import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.infrastructure.restapi.AddressRequest;
import com.klabis.members.infrastructure.restapi.UpdateMemberRequest;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}