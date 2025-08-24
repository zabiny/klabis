package club.klabis.users.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.RegistrationNumber;

public class ApplicationUserNotFound extends RuntimeException {
    public static ApplicationUserNotFound forMemberId(MemberId memberId) {
        return new ApplicationUserNotFound("Application user for member ID %s was not found".formatted(memberId));
    }

    public static ApplicationUserNotFound forRegistrationId(RegistrationNumber registrationNumber) {
        return new ApplicationUserNotFound("Application user with registration number %s was not found".formatted(registrationNumber.toRegistrationId()));
    }

    public ApplicationUserNotFound(String message) {
        super(message);
    }
}
