package club.klabis.domain.members.forms;

import club.klabis.domain.members.Contact;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Constraint(validatedBy = {
        AtLeastOneContactIsDefinedConstraint.MemberEditFormValidator.class,
        AtLeastOneContactIsDefinedConstraint.RegistrationFormValidator.class,
        AtLeastOneContactIsDefinedConstraint.EditOwnMemberInfoFormValidator.class,
        AtLeastOneContactIsDefinedConstraint.EditMyDetailsFormApiDtoFormValidator.class
})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AtLeastOneContactIsDefined.List.class)
public @interface AtLeastOneContactIsDefined {

    String message() default "At least one contact or guardian with contact must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Contact.Type contactType();

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        AtLeastOneContactIsDefined[] value();
    }
}
