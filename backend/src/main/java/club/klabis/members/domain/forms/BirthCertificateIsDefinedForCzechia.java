package club.klabis.members.domain.forms;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {
        BirthCertificateIsDefinedForCzechiaConstraint.RegistrationFormValidator.class,
        BirthCertificateIsDefinedForCzechiaConstraint.MemberEditFormValidator.class,
        BirthCertificateIsDefinedForCzechiaConstraint.EditAnotherMemberInfoByAdminFormValidator.class
})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BirthCertificateIsDefinedForCzechia {
    String message() default "Birth certificate number is required for CZ nationality";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}



