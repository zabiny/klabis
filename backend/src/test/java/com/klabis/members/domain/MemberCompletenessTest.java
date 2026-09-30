package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberCompleteness")
class MemberCompletenessTest {

    private static final LocalDate ADULT_DOB = LocalDate.now().minusYears(30);
    private static final LocalDate MINOR_DOB = LocalDate.now().minusYears(12);

    private static final GuardianContacts EMAIL_AND_PHONE = new GuardianContacts(true, true, true);
    private static final GuardianContacts ONLY_EMAIL = new GuardianContacts(true, true, false);
    private static final GuardianContacts ONLY_PHONE = new GuardianContacts(true, false, true);
    private static final GuardianContacts WITHOUT_CONTACTS = new GuardianContacts(true, false, false);

    @Nested
    @DisplayName("adult member")
    class Adult {

        @Test
        @DisplayName("complete with own e-mail, phone, address and birth number")
        void complete() {
            var member = aMember().withDateOfBirth(ADULT_DOB).withNationality("SK").build();

            assertThat(MemberCompleteness.missingData(member, GuardianContacts.NONE)).isEmpty();
        }

        @Test
        @DisplayName("never misses a guardian")
        void neverMissesGuardian() {
            var member = aMember().withDateOfBirth(ADULT_DOB).withNationality("SK").build();

            assertThat(MemberCompleteness.missingData(member, GuardianContacts.NONE))
                    .doesNotContain(MissingDataItem.GUARDIAN);
        }

        @Test
        @DisplayName("misses own e-mail and phone; guardian contacts do not count")
        void ownContactsOnly() {
            var member = aMember().withDateOfBirth(ADULT_DOB).withNationality("SK").withEmail((EmailAddress) null)
                    .withPhone((PhoneNumber) null).build();

            assertThat(MemberCompleteness.missingData(member, EMAIL_AND_PHONE))
                    .containsExactlyInAnyOrder(MissingDataItem.EMAIL, MissingDataItem.PHONE);
        }

        @Test
        @DisplayName("misses birth number when Czech")
        void birthNumberForCzech() {
            var member = aMember().withDateOfBirth(ADULT_DOB).withNationality("CZ").build();

            assertThat(MemberCompleteness.missingData(member, GuardianContacts.NONE))
                    .containsExactly(MissingDataItem.BIRTH_NUMBER);
        }

        @Test
        @DisplayName("misses address")
        void address() {
            var member = aMember().withDateOfBirth(ADULT_DOB).withNationality("SK").withAddress(null).build();

            assertThat(MemberCompleteness.missingData(member, GuardianContacts.NONE))
                    .containsExactly(MissingDataItem.ADDRESS);
        }
    }

    @Nested
    @DisplayName("minor member")
    class Minor {

        private Member minorWithoutContacts() {
            return aMember().withDateOfBirth(MINOR_DOB).withNationality("SK").withEmail((EmailAddress) null)
                    .withPhone((PhoneNumber) null).build();
        }

        @Test
        @DisplayName("misses GUARDIAN when not in a legal guardian group")
        void missesGuardianWithoutGroup() {
            var member = aMember().withDateOfBirth(MINOR_DOB).withNationality("SK").build();

            assertThat(MemberCompleteness.missingData(member, GuardianContacts.NONE))
                    .containsExactly(MissingDataItem.GUARDIAN);
        }

        @Test
        @DisplayName("complete with own contacts and a guardian")
        void completeWithGuardian() {
            var member = aMember().withDateOfBirth(MINOR_DOB).withNationality("SK").build();

            assertThat(MemberCompleteness.missingData(member, WITHOUT_CONTACTS)).isEmpty();
        }

        @Test
        @DisplayName("e-mail and phone are covered by a guardian")
        void contactsCoveredByGuardian() {
            assertThat(MemberCompleteness.missingData(minorWithoutContacts(), EMAIL_AND_PHONE)).isEmpty();
        }

        @Test
        @DisplayName("e-mail and phone may come from different guardians")
        void contactsFromDifferentGuardians() {
            var combined = ONLY_EMAIL.and(ONLY_PHONE);

            assertThat(MemberCompleteness.missingData(minorWithoutContacts(), combined)).isEmpty();
        }

        @Test
        @DisplayName("guardian without a phone does not cover the phone")
        void guardianWithoutPhone() {
            assertThat(MemberCompleteness.missingData(minorWithoutContacts(), ONLY_EMAIL))
                    .containsExactly(MissingDataItem.PHONE);
        }

        @Test
        @DisplayName("misses everything without guardian and own contacts")
        void missesEverything() {
            assertThat(MemberCompleteness.missingData(minorWithoutContacts(), GuardianContacts.NONE))
                    .isEqualTo(Set.of(MissingDataItem.EMAIL, MissingDataItem.PHONE, MissingDataItem.GUARDIAN));
        }
    }
}
