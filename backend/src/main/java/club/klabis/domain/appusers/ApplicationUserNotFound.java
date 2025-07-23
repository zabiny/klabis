package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.RegistrationNumber;

public class ApplicationUserNotFound extends RuntimeException {
    public static ApplicationUserNotFound forMemberId(Member.Id memberId) {
        return new ApplicationUserNotFound("Application user for member ID %s was not found".formatted(memberId));
    }

    public static ApplicationUserNotFound forRegistrationId(RegistrationNumber registrationNumber) {
        return new ApplicationUserNotFound("Application user with registration number %s was not found".formatted(registrationNumber.toRegistrationId()));
    }

    public ApplicationUserNotFound(String message) {
        super(message);
    }
}
