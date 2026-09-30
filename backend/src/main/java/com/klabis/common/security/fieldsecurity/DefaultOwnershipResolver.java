package com.klabis.common.security.fieldsecurity;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link OwnershipResolver} that uses {@link ConversionService} to convert
 * the owner identifier to UUID and compares it with the member ID from the JWT token.
 * <p>
 * Returns {@code false} when the authentication is not a {@link KlabisJwtAuthenticationToken}
 * or when the owner ID cannot be converted to UUID.
 */
@MvcComponent
class DefaultOwnershipResolver implements OwnershipResolver {

    private final ConversionService conversionService;

    DefaultOwnershipResolver(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean isOwner(Object ownerIdValue, Authentication authentication) {
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return false;
        }

        // A non-member (a legal guardian) has a user id but no member profile, and a member's id is its user id,
        // so the user id identifies the owner in both cases.
        Set<UUID> identities = new HashSet<>();
        identities.add(token.getUserId().uuid());
        token.getMemberIdUuid().ifPresent(identities::add);

        if (ownerIdValue instanceof Collection<?> ownerIds) {
            return ownerIds.stream().anyMatch(element -> identities.contains(toUuid(element)));
        }
        return identities.contains(toUuid(ownerIdValue));
    }

    private UUID toUuid(Object ownerIdValue) {
        if (ownerIdValue == null) {
            return null;
        }
        if (ownerIdValue instanceof UUID uuid) {
            return uuid;
        }
        if (!conversionService.canConvert(ownerIdValue.getClass(), UUID.class)) {
            return null;
        }
        return conversionService.convert(ownerIdValue, UUID.class);
    }
}
