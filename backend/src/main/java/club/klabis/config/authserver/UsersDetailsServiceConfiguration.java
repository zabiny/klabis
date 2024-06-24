package club.klabis.config.authserver;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
class UsersDetailsServiceConfiguration {

    private final ApplicationUsersRepository applicationUsersRepository;

    public UsersDetailsServiceConfiguration(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return applicationUsersRepository.findByUserName(username)
                        .map(this::fromMember)
                        .orElseThrow(() -> new UsernameNotFoundException("User with username %s not found".formatted(username)));
            }

            private UserDetails fromMember(ApplicationUser member) {
                return User.withUsername(member.getUsername()).password(member.getPassword()).build();
            }
        };
    }



}
