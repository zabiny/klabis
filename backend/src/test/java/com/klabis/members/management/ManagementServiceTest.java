package com.klabis.members.management;

import com.klabis.members.*;
import com.klabis.members.persistence.MemberRepository;
import com.klabis.users.UserId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ManagementService}.
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
    private MemberRepository memberRepository;

    private ManagementService service;

    @BeforeEach
    void setUp() {
        service = new ManagementService(memberRepository);
    }

    @AfterEach
    void tearDown() {
        // Clear SecurityContext after each test to avoid interference
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getMember() method")
    class GetMemberMethod {

        @Test
        @DisplayName("should return member details when member is found")
        void shouldReturnMemberDetailsWhenFound() {
            // Given
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId);

            when(memberRepository.findById(new UserId(memberId))).thenReturn(java.util.Optional.of(member));

            // When
            MemberDetailsDTO result = service.getMember(memberId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(memberId);
            assertThat(result.firstName()).isEqualTo("Jan");
            assertThat(result.lastName()).isEqualTo("Novák");
            assertThat(result.email()).isEqualTo("jan.novak@example.com");
            assertThat(result.registrationNumber()).isEqualTo("ZBM0501");
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            UUID memberId = UUID.randomUUID();
            when(memberRepository.findById(new UserId(memberId))).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getMember(memberId))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        private Member createTestMember(UUID memberId) {
            return MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420777888999")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withNoGuardian()
                    .build();
        }
    }

    @Nested
    @DisplayName("listMembers() method")
    class ListMembersMethod {

        @Test
        @DisplayName("should return page of member summaries")
        void shouldReturnPageOfMemberSummaries() {
            // Given
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 10);

            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .build();

            org.springframework.data.domain.Page<Member> memberPage =
                    new org.springframework.data.domain.PageImpl<>(java.util.List.of(member));
            when(memberRepository.findAll(pageable)).thenReturn(memberPage);

            // When
            org.springframework.data.domain.Page<MemberSummaryDTO> result = service.listMembers(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).firstName()).isEqualTo("Jan");
            assertThat(result.getContent().get(0).lastName()).isEqualTo("Novák");
            assertThat(result.getContent().get(0).registrationNumber()).isEqualTo("ZBM0501");
        }

        @Test
        @DisplayName("should return empty page when no members exist")
        void shouldReturnEmptyPageWhenNoMembers() {
            // Given
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 10);

            @SuppressWarnings("unchecked")
            org.springframework.data.domain.Page<Member> emptyPage =
                    new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList());
            when(memberRepository.findAll(pageable)).thenReturn(emptyPage);

            // When
            org.springframework.data.domain.Page<MemberSummaryDTO> result = service.listMembers(pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should pass pageable parameters to repository")
        void shouldPassPageableParametersToRepository() {
            // Given
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 10);

            @SuppressWarnings("unchecked")
            org.springframework.data.domain.Page<Member> emptyPage =
                    new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList());
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(emptyPage);

            // When
            service.listMembers(pageable);

            // Then
            ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                    ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
            verify(memberRepository).findAll(captor.capture());

            org.springframework.data.domain.Pageable captured = captor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0);
            assertThat(captured.getPageSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("updateMember() method")
    class UpdateMemberMethod {

        private UUID testMemberId;
        private Member testMember;
        private static final String MEMBER_REG_NUMBER = "ZBM9001";
        private static final String MEMBER_EMAIL = "jan.novak@example.com";

        @BeforeEach
        void setUp() {
            testMemberId = UUID.randomUUID();
            testMember = MemberTestDataBuilder.aMemberWithId(testMemberId)
                    .withRegistrationNumber(MEMBER_REG_NUMBER)
                    .withEmail(MEMBER_EMAIL)
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withPhone("+420123456789")
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withNoGuardian()
                    .build();
        }

        @Test
        @DisplayName("should update member email when authenticated admin")
        void shouldUpdateEmailWhenAdmin() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("new.email@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
            mockAdminAuthentication();

            // When
            UUID updatedId = service.updateMember(testMemberId, request);

            // Then
            assertThat(updatedId).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should throw InvalidUpdateException when update request is empty")
        void shouldThrowExceptionWhenUpdateIsEmpty() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            mockAdminAuthentication();

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(InvalidUpdateException.class)
                    .hasMessageContaining("at least one field to update");
        }

        @Test
        @DisplayName("should throw SelfEditNotAllowedException when non-admin tries to edit another member")
        void shouldThrowExceptionWhenNonAdminEditsAnotherMember() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("different@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            mockNonAdminAuthentication("ZBM9999"); // Different registration number

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(SelfEditNotAllowedException.class);
        }

        @Test
        @DisplayName("should throw AdminFieldAccessException when non-admin tries to edit admin-only fields")
        void shouldThrowExceptionWhenNonAdminEditsAdminFields() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.of("NewFirstName"), // Admin-only field
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            mockNonAdminAuthentication(MEMBER_REG_NUMBER); // Self-edit but with admin field

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(AdminFieldAccessException.class)
                    .hasMessageContaining("firstName");
        }

        @Test
        @DisplayName("should allow self-edit of member-editable fields")
        void shouldAllowSelfEditOfEditableFields() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("updated.email@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
            mockNonAdminAuthentication(MEMBER_REG_NUMBER); // Self-edit

            // When
            UUID updatedId = service.updateMember(testMemberId, request);

            // Then
            assertThat(updatedId).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @SuppressWarnings("unchecked")
        private void mockAdminAuthentication() {
            org.springframework.security.core.Authentication auth =
                    mock(org.springframework.security.core.Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            when(auth.getAuthorities()).thenReturn((Collection) java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("MEMBERS:UPDATE")
            ));
        }

        @SuppressWarnings("unchecked")
        private void mockNonAdminAuthentication(String registrationNumber) {
            org.springframework.security.core.Authentication auth =
                    mock(org.springframework.security.core.Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn(registrationNumber);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            when(auth.getAuthorities()).thenReturn((Collection) java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("MEMBERS:READ")
            ));
        }
    }

    @Nested
    @DisplayName("Edge cases and error handling")
    class EdgeCasesAndErrorHandling {

        private UUID testMemberId;

        @BeforeEach
        void setUp() {
            testMemberId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should throw InvalidUpdateException when member not found during update")
        void shouldThrowExceptionWhenMemberNotFound() {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("test@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.empty());
            mockAdminAuthentication();

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(InvalidUpdateException.class)
                    .hasMessageContaining("Member not found");
        }

        @Test
        @DisplayName("should throw InvalidUpdateException when user not authenticated")
        void shouldThrowExceptionWhenUserNotAuthenticated() {
            // Given
            Member testMember = createTestMember(testMemberId);
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("test@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            // Set up null authentication
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(null);

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(InvalidUpdateException.class)
                    .hasMessageContaining("authenticated");
        }

        @Test
        @DisplayName("should throw InvalidUpdateException when user authenticated but name is empty")
        void shouldThrowExceptionWhenUserNameIsEmpty() {
            // Given
            Member testMember = createTestMember(testMemberId);
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("test@example.com"),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));

            // Mock authentication with empty name
            org.springframework.security.core.Authentication auth =
                    mock(org.springframework.security.core.Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("");
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            // When & Then
            assertThatThrownBy(() -> service.updateMember(testMemberId, request))
                    .isInstanceOf(UserIdentificationException.class)
                    .hasMessageContaining("Unable to identify user");
        }

        @Test
        @DisplayName("should handle update with admin-only fields")
        void shouldHandleUpdateWithAdminOnlyFields() {
            // Given
            Member testMember = createTestMember(testMemberId);
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.empty(), // email
                    java.util.Optional.empty(), // phone
                    java.util.Optional.empty(), // address
                    java.util.Optional.of("UpdatedFirstName"), // firstName - admin field
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.of("CHIP123"), // chipNumber - admin field
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
            mockAdminAuthentication();

            // When
            UUID result = service.updateMember(testMemberId, request);

            // Then
            assertThat(result).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should handle update with only contact information")
        void shouldHandleUpdateWithOnlyContactInfo() {
            // Given
            Member testMember = createTestMember(testMemberId);
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.of("newemail@example.com"),
                    java.util.Optional.of("+420987654321"),
                    java.util.Optional.of(new AddressRequest("New Street 1", "New City", "11111", "CZ")),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty()
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
            mockAdminAuthentication();

            // When
            UUID result = service.updateMember(testMemberId, request);

            // Then
            assertThat(result).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should handle update with dietary restrictions")
        void shouldHandleUpdateWithDietaryRestrictions() {
            // Given
            Member testMember = createTestMember(testMemberId);
            UpdateMemberRequest request = new UpdateMemberRequest(
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.of("Vegetarian, Gluten-free")
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(java.util.Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
            mockAdminAuthentication();

            // When
            UUID result = service.updateMember(testMemberId, request);

            // Then
            assertThat(result).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        private Member createTestMember(UUID memberId) {
            return MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM9001")
                    .withEmail("jan.novak@example.com")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withPhone("+420123456789")
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withNoGuardian()
                    .build();
        }

        @SuppressWarnings("unchecked")
        private void mockAdminAuthentication() {
            org.springframework.security.core.Authentication auth =
                    mock(org.springframework.security.core.Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            when(auth.getAuthorities()).thenReturn((Collection) java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("MEMBERS:UPDATE")
            ));
        }
    }
}
