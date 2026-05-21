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

    private final CalendarFeedTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    IcalTokenService(CalendarFeedTokenRepository tokenRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<IcalTokenPort.TokenState> getTokenState(UserId userId) {
        Assert.notNull(userId, "userId must not be null");
        return tokenRepository.findByUserId(userId)
                .map(token -> new IcalTokenPort.TokenState(token.getTokenLookup(), token.getLastSetAt()));
    }

    @Override
    @Transactional
    public IcalTokenPort.GenerateResult generateOrRotate(UserId userId) {
        Assert.notNull(userId, "userId must not be null");
        Optional<CalendarFeedToken> existing = tokenRepository.findByUserId(userId);
        if (existing.isPresent()) {
            CalendarFeedToken token = existing.get();
            String raw = token.regenerate(passwordEncoder);
            CalendarFeedToken saved = tokenRepository.save(token);
            return new IcalTokenPort.GenerateResult(raw, saved.getLastSetAt());
        }

        CalendarFeedToken.Result result = CalendarFeedToken.generate(userId, passwordEncoder);
        CalendarFeedToken saved = tokenRepository.save(result.token());
        return new IcalTokenPort.GenerateResult(result.rawToken(), saved.getLastSetAt());
    }

    @Override
    public Optional<UserId> validate(String rawToken) {
        if (rawToken == null || rawToken.length() < CalendarFeedToken.LOOKUP_LENGTH) {
            return Optional.empty();
        }

        String lookup = rawToken.substring(0, CalendarFeedToken.LOOKUP_LENGTH);
        List<CalendarFeedToken> candidates = tokenRepository.findByTokenLookup(lookup);

        return candidates.stream()
                .filter(token -> passwordEncoder.matches(rawToken, token.getTokenHash()))
                .map(CalendarFeedToken::getUserId)
                .findFirst();
    }
}
