package com.klabis.common.users.application;

import com.klabis.common.users.domain.IncorrectCurrentPasswordException;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserNotFoundException;
import com.klabis.common.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PasswordChangeService implements PasswordChangePort {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordComplexityValidator passwordValidator;

    PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordComplexityValidator passwordValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }

        passwordValidator.validateBasic(command.newPassword());

        String newHash = passwordEncoder.encode(command.newPassword());
        User updated = user.changePassword(newHash);
        userRepository.save(updated);

        log.info("Password changed for user {}", command.userId());
    }
}
