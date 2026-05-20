package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarFeedToken;
import com.klabis.calendar.domain.CalendarFeedTokenRepository;
import com.klabis.common.users.UserId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service
class IcalTokenService implements IcalTokenPort {

    private static final int LOOKUP_LENGTH = 8;

    private final CalendarFeedTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    IcalTokenService(CalendarFeedTokenRepository tokenRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public String generate(UserId userId) {
        Assert.notNull(userId, "userId must not be null");
        return createOrRotate(userId);
    }

    @Override
    @Transactional
    public String regenerate(UserId userId) {
        Assert.notNull(userId, "userId must not be null");
        return createOrRotate(userId);
    }

    @Override
    public Optional<UserId> validate(String rawToken) {
        if (rawToken == null || rawToken.length() < LOOKUP_LENGTH) {
            return Optional.empty();
        }

        String lookup = rawToken.substring(0, LOOKUP_LENGTH);
        List<CalendarFeedToken> candidates = tokenRepository.findByTokenLookup(lookup);

        return candidates.stream()
                .filter(token -> passwordEncoder.matches(rawToken, token.getTokenHash()))
                .map(CalendarFeedToken::getUserId)
                .findFirst();
    }

    private String createOrRotate(UserId userId) {
        Optional<CalendarFeedToken> existing = tokenRepository.findByUserId(userId);
        if (existing.isPresent()) {
            CalendarFeedToken token = existing.get();
            String raw = token.regenerate(passwordEncoder);
            tokenRepository.save(token);
            return raw;
        }

        CalendarFeedToken.Result result = CalendarFeedToken.generate(userId, passwordEncoder);
        tokenRepository.save(result.token());
        return result.rawToken();
    }
}
