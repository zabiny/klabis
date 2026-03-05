package com.klabis.common.users.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.common.bootstrap.BootstrapProperties;
import com.klabis.common.bootstrap.PasswordGenerator;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class UsersDataBootstrap implements BootstrapDataInitializer {

    private final PasswordGenerator passwordGenerator;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrapProperties;

    private static final Logger LOG = LoggerFactory.getLogger(UsersDataBootstrap.class);

    UsersDataBootstrap(PasswordGenerator passwordGenerator, UserService userService, PasswordEncoder passwordEncoder, BootstrapProperties bootstrapProperties) {
        this.passwordGenerator = passwordGenerator;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Override
    public boolean requiresBootstrap() {
        return userService.findUserByUsername(bootstrapProperties.getUsername()).isEmpty();
    }

    @Override
    public void bootstrapData() {
        String username = bootstrapProperties.getUsername();
        String password = bootstrapProperties.getPassword();

        // Generate secure random password if not provided
        if (StringUtils.isBlank(password)) {
            LOG.warn("KLABIS_ADMIN_PASSWORD not set, generating random password.");
            password = passwordGenerator.generateSecurePassword();
            LOG.info("Generated bootstrap admin password for user '{}': {}", username, password);
        }

        String passwordHash = passwordEncoder.encode(password);

        Set<Authority> authorities = Set.of(Authority.values());

        userService.createActiveUser(username, passwordHash, authorities);

        LOG.info("Created bootstrap admin user: {} with all authorities", username);
    }
}