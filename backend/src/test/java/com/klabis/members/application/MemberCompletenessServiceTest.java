package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MissingDataItem;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardian.application.GuardianKind;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MemberCompletenessService")
@ExtendWith(MockitoExtension.class)
class MemberCompletenessServiceTest {

    private static final UserId GUARDIAN = new UserId(UUID.randomUUID());

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Mock
    private GuardianContactResolver guardianContactResolver;

    @InjectMocks
    private MemberCompletenessService service;

    private static Member adult() {
        return aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(30)).build();
    }

    private static Member minorWithoutContacts() {
        return aMemberWithId(UUID.randomUUID()).withDateOfBirth(LocalDate.now().minusYears(10))
                .withEmail((com.klabis.members.domain.EmailAddress) null).withPhone((com.klabis.members.domain.PhoneNumber) null).build();
    }

    @Test
    @DisplayName("does not resolve guardians of an adult")
    void skipsGuardiansOfAdult() {
        assertThat(service.guardianContactsOf(adult())).isEqualTo(GuardianContacts.NONE);
        assertThat(service.missingData(adult())).doesNotContain(MissingDataItem.GUARDIAN);

        verifyNoInteractions(legalGuardianGroupPort, guardianContactResolver);
    }

    @Test
    @DisplayName("does not resolve given guardians for an adult")
    void skipsGivenGuardiansOfAdult() {
        service.missingData(adult(), Set.of(GUARDIAN));

        verify(guardianContactResolver, never()).resolve(any());
    }

    @Test
    @DisplayName("covers a minor's contacts by the contacts of their guardians")
    void minorCoveredByGuardians() {
        Member minor = minorWithoutContacts();
        when(guardianContactResolver.resolve(Set.of(GUARDIAN))).thenReturn(List.of(
                new GuardianContact(GUARDIAN, "Petr", "Novák", "petr@example.com", "+420777111222", GuardianKind.MEMBER)));

        assertThat(service.missingData(minor, Set.of(GUARDIAN)))
                .doesNotContain(MissingDataItem.GUARDIAN, MissingDataItem.EMAIL, MissingDataItem.PHONE);
    }

    @Test
    @DisplayName("rejects a chosen guardian who cannot serve as one")
    void rejectsUnusableChosenGuardian() {
        when(guardianContactResolver.resolve(Set.of(GUARDIAN))).thenReturn(List.of());

        assertThatThrownBy(() -> service.contactsOfChosenGuardians(Set.of(GUARDIAN)))
                .isInstanceOf(GuardianNotFoundException.class);
    }

    @Test
    @DisplayName("misses the guardian of a minor without a group")
    void minorWithoutGuardian() {
        Member minor = minorWithoutContacts();
        when(legalGuardianGroupPort.guardiansOf(minor.getId())).thenReturn(Set.of());

        assertThat(service.missingData(minor)).contains(MissingDataItem.GUARDIAN);
    }
}
