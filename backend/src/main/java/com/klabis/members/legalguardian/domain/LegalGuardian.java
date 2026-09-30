package com.klabis.members.legalguardian.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.PersonName;
import com.klabis.members.domain.PhoneNumber;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

/**
 * Legal guardian who is not a club member. A user is either a Member or a LegalGuardian, never both.
 * E-mail and phone are always present and can be changed but never cleared.
 */
@AggregateRoot
public class LegalGuardian extends KlabisAggregateRoot<LegalGuardian, UserId> {

    @Identity
    private final UserId id;

    private PersonName name;
    private EmailAddress email;
    private PhoneNumber phone;

    public record CreateLegalGuardian(UserId id, String firstName, String lastName, String email, String phone) {
    }

    /**
     * Null field means "keep the current value"; a blank one is rejected since contact details cannot be cleared.
     */
    public record UpdateLegalGuardian(String firstName, String lastName, String email, String phone) {
    }

    private LegalGuardian(UserId id, PersonName name, EmailAddress email, PhoneNumber phone) {
        Assert.notNull(id, "LegalGuardian id is required");
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public static LegalGuardian create(CreateLegalGuardian command) {
        return new LegalGuardian(command.id(),
                PersonName.of(command.firstName(), command.lastName()),
                EmailAddress.of(command.email()),
                PhoneNumber.of(command.phone()));
    }

    public static LegalGuardian reconstruct(UserId id, PersonName name, EmailAddress email, PhoneNumber phone,
                                            AuditMetadata auditMetadata) {
        LegalGuardian guardian = new LegalGuardian(id, name, email, phone);
        guardian.updateAuditMetadata(auditMetadata);
        return guardian;
    }

    public void update(UpdateLegalGuardian command) {
        PersonName newName = PersonName.of(
                command.firstName() != null ? command.firstName() : name.firstName(),
                command.lastName() != null ? command.lastName() : name.lastName());
        EmailAddress newEmail = command.email() != null ? EmailAddress.of(command.email()) : email;
        PhoneNumber newPhone = command.phone() != null ? PhoneNumber.of(command.phone()) : phone;

        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
    }

    @Override
    public UserId getId() {
        return id;
    }

    public String getFirstName() {
        return name.firstName();
    }

    public String getLastName() {
        return name.lastName();
    }

    public PersonName getName() {
        return name;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public PhoneNumber getPhone() {
        return phone;
    }
}
