package club.klabis.users.domain.events;

import club.klabis.users.domain.ApplicationUser;

public record ApplicationUserRegistered(ApplicationUser.Id id, ApplicationUser.UserName username) {

}
