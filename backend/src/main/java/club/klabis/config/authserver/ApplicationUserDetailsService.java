package club.klabis.config.authserver;

import club.klabis.application.users.ApplicationUsersRepository;
import club.klabis.domain.users.ApplicationUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
class ApplicationUserDetailsService implements UserDetailsService {

    private final ApplicationUsersRepository applicationUsersRepository;

    public ApplicationUserDetailsService(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return applicationUsersRepository.findByUserName(username)
                .map(this::fromMember)
                .orElseThrow(() -> new UsernameNotFoundException("User with username %s not found".formatted(username)));
    }

    private UserDetails fromMember(ApplicationUser member) {
        return User.withUsername(member.getUsername())
                .password(member.getPassword())
                .disabled(member.isDisabled())
                .build();
    }

}
