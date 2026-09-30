package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.application.PasswordSetupService;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.GeneratedTokenResult;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.testdata.UserTestDataBuilder;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberAccountActivationService tests")
class MemberAccountActivationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordSetupService passwordSetupService;

    private MemberAccountActivationService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MemberAccountActivationService(memberRepository, userService, passwordSetupService);
    }

    private Member minor(String email) {
        return MemberTestDataBuilder.aMemberWithId(id)
                .withFirstName("Karel")
                .withDateOfBirth(LocalDate.now().minusYears(12))
                .withEmail(email == null ? null : EmailAddress.of(email))
                .build();
    }

    private Member adult(String email) {
        return MemberTestDataBuilder.aMemberWithId(id)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withEmail(email)
                .build();
    }

    private User userWithStatus(AccountStatus status) {
        return UserTestDataBuilder.aPendingUser().withId(new UserId(id)).status(status).build();
    }

    @Nested
    @DisplayName("isAvailableFor")
    class IsAvailableFor {

        @Test
        @DisplayName("true for a minor with an own e-mail and an account awaiting activation")
        void availableForMinorWithEmailAndPendingAccount() {
            when(userService.findUserById(new UserId(id))).thenReturn(Optional.of(userWithStatus(AccountStatus.PENDING_ACTIVATION)));

            assertThat(service.isAvailableFor(minor("child@example.com"))).isTrue();
        }

        @Test
        @DisplayName("false for a minor without an own e-mail")
        void notAvailableWithoutEmail() {
            assertThat(service.isAvailableFor(minor(null))).isFalse();
        }

        @Test
        @DisplayName("false for an adult")
        void notAvailableForAdult() {
            assertThat(service.isAvailableFor(adult("adult@example.com"))).isFalse();
        }

        @Test
        @DisplayName("false when the account is already active")
        void notAvailableForActiveAccount() {
            when(userService.findUserById(new UserId(id))).thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));

            assertThat(service.isAvailableFor(minor("child@example.com"))).isFalse();
        }

        @Test
        @DisplayName("false when the user account does not exist")
        void notAvailableWithoutAccount() {
            when(userService.findUserById(new UserId(id))).thenReturn(Optional.empty());

            assertThat(service.isAvailableFor(minor("child@example.com"))).isFalse();
        }
    }

    @Nested
    @DisplayName("sendAccountActivation")
    class SendAccountActivation {

        @Test
        @DisplayName("generates a token and sends the link to the minor's own e-mail")
        void sendsLinkToMinorsEmail() {
            User user = userWithStatus(AccountStatus.PENDING_ACTIVATION);
            when(memberRepository.findById(new MemberId(id))).thenReturn(Optional.of(minor("child@example.com")));
            when(userService.findUserById(new UserId(id))).thenReturn(Optional.of(user));
            when(passwordSetupService.generateToken(user)).thenReturn(new GeneratedTokenResult(null, "plain-token"));

            service.sendAccountActivation(new MemberId(id));

            verify(passwordSetupService).sendPasswordSetupEmail("Karel", "child@example.com", "plain-token");
        }

        @Test
        @DisplayName("fails with a business rule violation for an adult and sends nothing")
        void rejectsAdult() {
            when(memberRepository.findById(new MemberId(id))).thenReturn(Optional.of(adult("adult@example.com")));

            assertThatThrownBy(() -> service.sendAccountActivation(new MemberId(id)))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(passwordSetupService, never()).generateToken(any());
        }

        @Test
        @DisplayName("fails with a business rule violation when the account is already active")
        void rejectsActiveAccount() {
            when(memberRepository.findById(new MemberId(id))).thenReturn(Optional.of(minor("child@example.com")));
            when(userService.findUserById(new UserId(id))).thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));

            assertThatThrownBy(() -> service.sendAccountActivation(new MemberId(id)))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(passwordSetupService, never()).generateToken(any());
        }

        @Test
        @DisplayName("fails with MemberNotFoundException for an unknown member")
        void rejectsUnknownMember() {
            when(memberRepository.findById(new MemberId(id))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendAccountActivation(new MemberId(id)))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}
