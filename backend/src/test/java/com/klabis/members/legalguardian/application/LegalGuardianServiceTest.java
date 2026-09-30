package com.klabis.members.legalguardian.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LegalGuardianService")
@ExtendWith(MockitoExtension.class)
class LegalGuardianServiceTest {

    private static final UserId USER_ID = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final LegalGuardianPort.NewLegalGuardian NEW_GUARDIAN = new LegalGuardianPort.NewLegalGuardian(
            "Jan", "Novák", "jan@example.com", "+420 777 123 456");

    @Mock
    private LegalGuardianRepository legalGuardianRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private UserService userService;
    @Mock
    private LoginNumberSequence loginNumberSequence;

    private LegalGuardianService service;

    @BeforeEach
    void setUp() {
        service = new LegalGuardianService(legalGuardianRepository, memberRepository, userService, loginNumberSequence);
    }

    private void givenUserAccount(String loginName) {
        when(userService.findUserById(USER_ID))
                .thenReturn(Optional.of(User.reconstruct(USER_ID, loginName, "hash", AccountStatus.PENDING_ACTIVATION)));
    }

    private static LegalGuardian existingGuardian() {
        return LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                USER_ID, "Jan", "Novák", "jan@example.com", "+420 777 123 456"));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates an account with the next EXT login number, holding only MEMBERS:READ")
        void createsAccountWithLoginNumber() {
            when(loginNumberSequence.next()).thenReturn("EXT0008");
            when(userService.createUser("EXT0008", Set.of(Authority.MEMBERS_READ))).thenReturn(USER_ID);
            when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(inv -> inv.getArgument(0));

            LegalGuardianProfile profile = service.register(NEW_GUARDIAN);

            assertThat(profile.loginName()).isEqualTo("EXT0008");
            assertThat(profile.guardian().getId()).isEqualTo(USER_ID);
            assertThat(profile.guardian().getEmail().value()).isEqualTo("jan@example.com");
        }

        @Test
        @DisplayName("rejects an e-mail of an existing legal guardian")
        void rejectsEmailOfExistingGuardian() {
            when(legalGuardianRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(existingGuardian()));

            assertThatThrownBy(() -> service.register(NEW_GUARDIAN))
                    .isInstanceOf(LegalGuardianEmailAlreadyInUseException.class);
            verify(userService, never()).createUser(anyString(), any());
        }

        @Test
        @DisplayName("rejects an e-mail of an adult member")
        void rejectsEmailOfAdultMember() {
            when(memberRepository.findAllByEmail("jan@example.com")).thenReturn(List.of(
                    aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(30)).build()));

            assertThatThrownBy(() -> service.register(NEW_GUARDIAN))
                    .isInstanceOf(LegalGuardianEmailAlreadyInUseException.class);
            verify(userService, never()).createUser(anyString(), any());
        }

        @Test
        @DisplayName("accepts an e-mail that only a minor member uses")
        void acceptsEmailOfMinorMember() {
            when(memberRepository.findAllByEmail("jan@example.com")).thenReturn(List.of(
                    aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(10)).build()));
            when(loginNumberSequence.next()).thenReturn("EXT0001");
            when(userService.createUser(anyString(), any())).thenReturn(USER_ID);
            when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.register(NEW_GUARDIAN).guardian().getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("rejects a guardian without phone before anything is created")
        void rejectsMissingPhone() {
            when(loginNumberSequence.next()).thenReturn("EXT0001");
            when(userService.createUser(anyString(), any())).thenReturn(USER_ID);

            assertThatThrownBy(() -> service.register(new LegalGuardianPort.NewLegalGuardian(
                    "Jan", "Novák", "jan@example.com", null)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(legalGuardianRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("get and update")
    class GetAndUpdate {

        @Test
        @DisplayName("returns the guardian together with the login number")
        void returnsProfile() {
            when(legalGuardianRepository.findById(USER_ID)).thenReturn(Optional.of(existingGuardian()));
            givenUserAccount("EXT0001");

            LegalGuardianProfile profile = service.get(USER_ID);

            assertThat(profile.loginName()).isEqualTo("EXT0001");
            assertThat(profile.guardian().getLastName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("throws when the guardian does not exist")
        void throwsWhenMissing() {
            when(legalGuardianRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(USER_ID)).isInstanceOf(LegalGuardianNotFoundException.class);
        }

        @Test
        @DisplayName("saves the updated contact details")
        void savesUpdate() {
            when(legalGuardianRepository.findById(USER_ID)).thenReturn(Optional.of(existingGuardian()));
            when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(inv -> inv.getArgument(0));
            givenUserAccount("EXT0001");

            service.update(USER_ID, new LegalGuardian.UpdateLegalGuardian(null, null, null, "+420 601 000 000"));

            ArgumentCaptor<LegalGuardian> saved = ArgumentCaptor.forClass(LegalGuardian.class);
            verify(legalGuardianRepository).save(saved.capture());
            assertThat(saved.getValue().getPhone().value()).isEqualTo("+420 601 000 000");
        }

        @Test
        @DisplayName("rejects changing the e-mail to one already in use")
        void rejectsEmailInUse() {
            when(legalGuardianRepository.findById(USER_ID)).thenReturn(Optional.of(existingGuardian()));
            LegalGuardian other = LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                    new UserId(UUID.randomUUID()), "Eva", "Svobodová", "eva@example.com", "+420 601 000 000"));
            when(legalGuardianRepository.findByEmail("eva@example.com")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.update(USER_ID,
                    new LegalGuardian.UpdateLegalGuardian(null, null, "eva@example.com", null)))
                    .isInstanceOf(LegalGuardianEmailAlreadyInUseException.class);
            verify(legalGuardianRepository, never()).save(any());
        }

        @Test
        @DisplayName("keeps the guardian's own unchanged e-mail without a uniqueness check")
        void keepsOwnEmail() {
            when(legalGuardianRepository.findById(USER_ID)).thenReturn(Optional.of(existingGuardian()));
            when(legalGuardianRepository.save(any(LegalGuardian.class))).thenAnswer(inv -> inv.getArgument(0));
            givenUserAccount("EXT0001");

            service.update(USER_ID, new LegalGuardian.UpdateLegalGuardian(null, null, "jan@example.com", null));

            verify(legalGuardianRepository, never()).findByEmail(anyString());
        }
    }
}
