package club.klabis.domain.appusers;

public class ApplicationUserNotFound extends RuntimeException {
    public static ApplicationUserNotFound forMemberId(Integer memberId) {
        return new ApplicationUserNotFound("Application user for member ID %s was not found".formatted(memberId));
    }

    public ApplicationUserNotFound(String message) {
        super(message);
    }
}
