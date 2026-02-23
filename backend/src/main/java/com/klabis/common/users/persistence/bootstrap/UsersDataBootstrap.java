package com.klabis.common.users.persistence.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.common.bootstrap.PasswordGenerator;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Required environment variables (with defaults):
 * - BOOTSTRAP_ADMIN_USERNAME: Admin registration number (default: "admin")
 * - BOOTSTRAP_ADMIN_PASSWORD: Admin password (default: generate random)
 * - BOOTSTRAP_ADMIN_ID: Admin user UUID (default: random UUID)
 */
@Component
class UsersDataBootstrap implements BootstrapDataInitializer {

    private final PasswordGenerator passwordGenerator;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @Value("${bootstrap.admin.username:" + DEFAULT_ADMIN_USERNAME + "}")
    private String adminUsername;
    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    private static final Logger LOG = LoggerFactory.getLogger(UsersDataBootstrap.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    UsersDataBootstrap(PasswordGenerator passwordGenerator, UserService userService, PasswordEncoder passwordEncoder) {
        this.passwordGenerator = passwordGenerator;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean requiresBootstrap() {
        return userService.findUserByUsername(adminUsername).isEmpty();
    }

    @Override
    public void bootstrapData() {
        String username = adminUsername;
        String password = adminPassword;

        // Generate secure random password if not provided
        if (StringUtils.isBlank(password)) {
            LOG.warn(
                    "BOOTSTRAP_ADMIN_PASSWORD not set, generating random password. Check logs for the generated password.");
            password = passwordGenerator.generateSecurePassword();
            LOG.info("Generated bootstrap admin password for user '{}': {}",
                    username, password);
            LOG.warn(
                    "Please save this password securely and set BOOTSTRAP_ADMIN_PASSWORD environment variable for future deployments");
        }

        String passwordHash = passwordEncoder.encode(password);

        Set<Authority> authorities = Set.of(Authority.values());

        userService.createActiveUser(username, passwordHash, authorities);

        LOG.info("Created bootstrap admin user: {} with all authorities", username);
    }
}