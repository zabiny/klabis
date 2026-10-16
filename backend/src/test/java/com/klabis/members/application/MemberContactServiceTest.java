package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardian.application.GuardianKind;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MemberContactService")
@ExtendWith(MockitoExtension.class)
class MemberContactServiceTest {

    private static final UserId NOVAK = new UserId(UUID.randomUUID());
    private static final UserId SVOBODOVA = new UserId(UUID.randomUUID());

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Mock
    private GuardianContactResolver guardianContactResolver;

    @InjectMocks
    private MemberContactService service;

    private static Member adult() {
        return aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(30))
                .withEmail("adult@example.com").withPhone("+420777000111").build();
    }

    private static Member minor(String email, String phone) {
        return aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(10))
                .withEmail(email == null ? null : EmailAddress.of(email))
                .withPhone(phone == null ? null : PhoneNumber.of(phone)).build();
    }

    private static GuardianContact novak(String email, String phone) {
        return new GuardianContact(NOVAK, "Jan", "Novák", email, phone, GuardianKind.MEMBER);
    }

    private static GuardianContact svobodova(String email, String phone) {
        return new GuardianContact(SVOBODOVA, "Eva", "Svobodová", email, phone, GuardianKind.LEGAL_GUARDIAN);
    }

    private void guardiansAre(GuardianContact... contacts) {
        when(guardianContactResolver.resolve(any())).thenReturn(List.of(contacts));
    }

    @Test
    @DisplayName("adult shows own contact and guardians are not consulted")
    void adultOwnContact() {
        EffectiveContact contact = service.effectiveContactOf(adult(), Set.of(NOVAK));

        assertThat(contact.email()).isEqualTo(new EffectiveContact.Value("adult@example.com", null));
        assertThat(contact.phone()).isEqualTo(new EffectiveContact.Value("+420777000111", null));
        verifyNoInteractions(guardianContactResolver);
    }

    @Test
    @DisplayName("minor with own contact ignores guardians")
    void minorOwnContact() {
        EffectiveContact contact = service.effectiveContactOf(minor("kid@example.com", "+420777222333"), Set.of(NOVAK));

        assertThat(contact.email().value()).isEqualTo("kid@example.com");
        assertThat(contact.phone().value()).isEqualTo("+420777222333");
        assertThat(contact.email().fromGuardian()).isFalse();
        verifyNoInteractions(guardianContactResolver);
    }

    @Test
    @DisplayName("minor without own contact takes the guardian's, marked with guardian name")
    void minorFromGuardian() {
        guardiansAre(novak("jan@example.com", "+420777444555"));

        EffectiveContact contact = service.effectiveContactOf(minor(null, null), Set.of(NOVAK));

        assertThat(contact.email()).isEqualTo(new EffectiveContact.Value("jan@example.com", "Jan Novák"));
        assertThat(contact.phone()).isEqualTo(new EffectiveContact.Value("+420777444555", "Jan Novák"));
        assertThat(contact.email().fromGuardian()).isTrue();
    }

    @Test
    @DisplayName("e-mail and phone are resolved independently")
    void independentResolution() {
        guardiansAre(svobodova(null, "+420777666777"), novak("jan@example.com", null));

        EffectiveContact contact = service.effectiveContactOf(minor(null, "+420777222333"), Set.of(NOVAK, SVOBODOVA));

        assertThat(contact.phone()).isEqualTo(new EffectiveContact.Value("+420777222333", null));
        assertThat(contact.email()).isEqualTo(new EffectiveContact.Value("jan@example.com", "Jan Novák"));
    }

    @Test
    @DisplayName("first guardian by surname wins")
    void firstBySurname() {
        guardiansAre(svobodova("eva@example.com", null), novak("jan@example.com", null));

        EffectiveContact contact = service.effectiveContactOf(minor(null, null), Set.of(NOVAK, SVOBODOVA));

        assertThat(contact.email().value()).isEqualTo("jan@example.com");
    }

    @Test
    @DisplayName("blank guardian value does not count")
    void blankGuardianValueSkipped() {
        guardiansAre(novak(" ", null), svobodova("eva@example.com", null));

        EffectiveContact contact = service.effectiveContactOf(minor(null, null), Set.of(NOVAK, SVOBODOVA));

        assertThat(contact.email()).isEqualTo(new EffectiveContact.Value("eva@example.com", "Eva Svobodová"));
    }

    @Test
    @DisplayName("nobody has the detail")
    void noValue() {
        guardiansAre(novak("jan@example.com", null));

        EffectiveContact contact = service.effectiveContactOf(minor(null, null), Set.of(NOVAK));

        assertThat(contact.phone().value()).isNull();
        assertThat(contact.phone().fromGuardian()).isFalse();
    }

    @Test
    @DisplayName("looks up guardians of the member when not given")
    void looksUpGuardians() {
        Member minor = minor(null, null);
        when(legalGuardianGroupPort.guardiansOf(minor.getId())).thenReturn(Set.of(NOVAK));
        guardiansAre(novak("jan@example.com", null));

        assertThat(service.effectiveContactOf(minor).email().value()).isEqualTo("jan@example.com");
    }
}
