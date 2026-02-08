package com.klabis.members;

import org.assertj.core.api.AbstractAssert;

import java.time.LocalDate;

public class MemberAssert extends AbstractAssert<MemberAssert, Member> {

    private MemberAssert(Member actual) {
        super(actual, MemberAssert.class);
    }

    public static MemberAssert assertThat(Member actual) {
        return new MemberAssert(actual);
    }

    public MemberAssert hasFirstName(String expected) {
        isNotNull();
        if (!actual.getFirstName().equals(expected)) {
            failWithMessage("Expected first name to be <%s> but was <%s>", expected, actual.getFirstName());
        }
        return this;
    }

    public MemberAssert hasLastName(String expected) {
        isNotNull();
        if (!actual.getLastName().equals(expected)) {
            failWithMessage("Expected last name to be <%s> but was <%s>", expected, actual.getLastName());
        }
        return this;
    }

    public MemberAssert hasRegistrationNumber(RegistrationNumber expected) {
        isNotNull();
        if (!actual.getRegistrationNumber().equals(expected)) {
            failWithMessage("Expected registration number to be <%s> but was <%s>",
                    expected,
                    actual.getRegistrationNumber());
        }
        return this;
    }

    public MemberAssert hasEmail(EmailAddress expected) {
        isNotNull();
        if (!actual.getEmail().equals(expected)) {
            failWithMessage("Expected email to be <%s> but was <%s>", expected, actual.getEmail());
        }
        return this;
    }

    public MemberAssert hasDateOfBirth(LocalDate expected) {
        isNotNull();
        if (!actual.getDateOfBirth().equals(expected)) {
            failWithMessage("Expected date of birth to be <%s> but was <%s>", expected, actual.getDateOfBirth());
        }
        return this;
    }

    public MemberAssert hasNationality(String expected) {
        isNotNull();
        if (!actual.getNationality().equals(expected)) {
            failWithMessage("Expected nationality to be <%s> but was <%s>", expected, actual.getNationality());
        }
        return this;
    }

    public MemberAssert hasGender(Gender expected) {
        isNotNull();
        if (actual.getGender() != expected) {
            failWithMessage("Expected gender to be <%s> but was <%s>", expected, actual.getGender());
        }
        return this;
    }

    public MemberAssert hasPhone(PhoneNumber expected) {
        isNotNull();
        if (!actual.getPhone().equals(expected)) {
            failWithMessage("Expected phone to be <%s> but was <%s>", expected, actual.getPhone());
        }
        return this;
    }

    public MemberAssert hasAddress(Address expected) {
        isNotNull();
        if (!actual.getAddress().equals(expected)) {
            failWithMessage("Expected address to be <%s> but was <%s>", expected, actual.getAddress());
        }
        return this;
    }

    public MemberAssert hasGuardian(GuardianInformation expected) {
        isNotNull();
        if (expected == null) {
            if (actual.getGuardian() != null) {
                failWithMessage("Expected guardian to be null but was <%s>", actual.getGuardian());
            }
        } else {
            if (actual.getGuardian() == null) {
                failWithMessage("Expected guardian to be <%s> but was null", expected);
            } else if (!actual.getGuardian().equals(expected)) {
                failWithMessage("Expected guardian to be <%s> but was <%s>", expected, actual.getGuardian());
            }
        }
        return this;
    }

    public MemberAssert hasGuardianNotNull() {
        isNotNull();
        if (actual.getGuardian() == null) {
            failWithMessage("Expected guardian to be not null");
        }
        return this;
    }

    public MemberAssert isActive() {
        isNotNull();
        if (!actual.isActive()) {
            failWithMessage("Expected member to be active");
        }
        return this;
    }

    public MemberAssert isInactive() {
        isNotNull();
        if (actual.isActive()) {
            failWithMessage("Expected member to be inactive");
        }
        return this;
    }
}
