package com.klabis.calendar.infrastructure.security;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.users.UserId;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * Validates {@link IcalTokenAuthenticationToken} against the calendar module's token store.
 * <p>
 * Delegates to {@link IcalTokenPort#validate(String)} — looks up by indexed prefix then
 * bcrypt-matches — and returns an authenticated token carrying the owner's {@link UserId}.
 * Throws {@link BadCredentialsException} for unknown or invalid tokens.
 */
class IcalTokenAuthenticationProvider implements AuthenticationProvider {

    private final IcalTokenPort icalTokenPort;

    IcalTokenAuthenticationProvider(IcalTokenPort icalTokenPort) {
        this.icalTokenPort = icalTokenPort;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        IcalTokenAuthenticationToken token = (IcalTokenAuthenticationToken) authentication;
        String rawToken = token.getRawToken();

        UserId userId = icalTokenPort.validate(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or unknown iCal token"));

        return new IcalTokenAuthenticationToken(userId, List.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return IcalTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
