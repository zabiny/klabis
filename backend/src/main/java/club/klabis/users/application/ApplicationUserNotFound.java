package club.klabis.users.application;

import club.klabis.users.domain.ApplicationUser;

public class ApplicationUserNotFound extends RuntimeException {

    public static ApplicationUserNotFound forUserId(ApplicationUser.Id id) {
        return new ApplicationUserNotFound("Application user with id %s was not found".formatted(id.value()));
    }

    public ApplicationUserNotFound(String message) {
        super(message);
    }
}
