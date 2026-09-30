package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Member#importFromOris(Member.RegisterMember)} (design.md D5, tasks.md 5.1):
 * unlike {@link Member#register(Member.RegisterMember)}, completeness rules are not enforced — a
 * member may be created missing any combination of {@link MissingDataItem} — but the birth-number
 * consistency rule (never allowed for a non-Czech national) still holds, since it is a consistency
 * rule rather than a completeness rule.
 */
@DisplayName("Member.importFromOris")
class MemberImportFromOrisTest {

    @Test
    @DisplayName("creates a member missing every completeness item at once")
    void createsMemberMissingEveryItem() {
        Member member = Member.importFromOris(aMember()
                .withDateOfBirth(LocalDate.now().minusYears(10))
                .withNationality("SK")
                .withEmail((EmailAddress) null)
                .withPhone((PhoneNumber) null)
                .withBirthNumber((BirthNumber) null)
                .toRegisterMemberCommand());

        assertThat(missing(member)).containsExactlyInAnyOrder(
                MissingDataItem.EMAIL, MissingDataItem.PHONE, MissingDataItem.GUARDIAN);
        assertThat(missing(member).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("creates a member missing only its address when ORIS's address was incomplete")
    void createsMemberMissingOnlyAddress() {
        Member member = Member.importFromOris(aMember()
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("CZ")
                .withBirthNumber("900515/1234")
                .withAddress(null)
                .toRegisterMemberCommand());

        assertThat(missing(member)).containsExactly(MissingDataItem.ADDRESS);
        assertThat(member.getAddress()).isNull();
        assertThat(missing(member).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("creates an adult CZ member missing only a birth number")
    void createsMemberMissingOnlyBirthNumber() {
        Member member = Member.importFromOris(aMember()
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("CZ")
                .withBirthNumber((BirthNumber) null)
                .toRegisterMemberCommand());

        assertThat(missing(member)).containsExactly(MissingDataItem.BIRTH_NUMBER);
    }

    @Test
    @DisplayName("creates a fully complete member when the ORIS data has everything")
    void createsCompleteMemberWhenDataIsComplete() {
        Member member = Member.importFromOris(aMember()
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("CZ")
                .withBirthNumber("900515/1234")
                .toRegisterMemberCommand());

        assertThat(missing(member).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("still refuses a birth number for a non-Czech national (consistency rule)")
    void stillRefusesBirthNumberForNonCzechNational() {
        Member.RegisterMember command = aMember()
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("SK")
                .withBirthNumber("900515/1234")
                .toRegisterMemberCommand();

        assertThatThrownBy(() -> Member.importFromOris(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Birth number is only allowed for Czech nationals");
    }

    private static Set<MissingDataItem> missing(Member member) {
        return MemberCompleteness.missingData(member, GuardianContacts.NONE);
    }
}
