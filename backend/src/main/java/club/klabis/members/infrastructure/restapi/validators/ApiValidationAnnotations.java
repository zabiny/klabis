package club.klabis.members.infrastructure.restapi.validators;

import club.klabis.members.domain.Contact;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

public class ApiValidationAnnotations {

    @Constraint(validatedBy = {EditMyDetailsFormApiDtoFormValidator.class})
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(AtLeastOneContactIsDefinedForApi.List.class)
    public @interface AtLeastOneContactIsDefinedForApi {
        String message() default "At least one contact or guardian with contact must be provided";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        Contact.Type contactType();

        @Target({ElementType.TYPE})
        @Retention(RetentionPolicy.RUNTIME)
        @interface List {
            AtLeastOneContactIsDefinedForApi[] value();
        }
    }

    @Constraint(validatedBy = {EditAnotherMemberDetailsFormApiDtoValidator.class})
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BirthCertificateIsDefinedForCzechiaForApi {
        String message() default "Birth certificate number is required for CZ nationality";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }
}
