package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;

public class ApplicationUserNotFound extends RuntimeException {
    public static ApplicationUserNotFound forMemberId(Member.Id memberId) {
        return new ApplicationUserNotFound("Application user for member ID %s was not found".formatted(memberId));
    }

    public ApplicationUserNotFound(String message) {
        super(message);
    }
}
