package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.members.MemberId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Registers {@link MemberId} as a UUID-convertible type with Spring's {@code ConversionService}.
 * <p>
 * Required by {@code DefaultOwnershipResolver} in the common security module, which converts
 * owner identifier values to UUID generically — avoiding a direct dependency from common to members.
 */
@Component
class MemberIdToUuidConverter implements Converter<MemberId, UUID> {

    @Override
    public UUID convert(MemberId source) {
        return source.value();
    }
}
