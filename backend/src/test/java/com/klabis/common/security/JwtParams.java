package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public record JwtParams(String userName, UserId userId, UUID memberId, Authority... authorities) {

    public static JwtParams jwtTokenParams(CurrentUserData currentUserData) {
        return jwtTokenParams(currentUserData.userName(),
                currentUserData.userId()).withMemberId(currentUserData.memberId());
    }

    public static JwtParams jwtTokenParams(String userName, UUID userId) {
        return new JwtParams(userName, new UserId(userId), null);
    }

    public static JwtParams jwtTokenParams(String userName, UserId userId) {
        return new JwtParams(userName, userId, null);
    }

    public static JwtParams member(UUID memberId) {
        return member(new MemberId(memberId));
    }

    public static JwtParams member(MemberId memberId) {
        return JwtParams.jwtTokenParams("ZBM8001", memberId.toUserId()).withMemberId(memberId);
    }

    public JwtParams withMemberId(MemberId memberId) {
        return new JwtParams(userName, userId, memberId != null ? memberId.uuid() : null, authorities);
    }

    public JwtParams withMemberId(UUID memberId) {
        return new JwtParams(userName, userId, memberId, authorities);
    }

    public JwtParams withAuthorities(Authority... authorities) {
        return new JwtParams(userName, userId, memberId, authorities);
    }

    public JwtParams addAuthority(Authority authority) {
        Authority[] newAuthorities = Stream.concat(Stream.of(authorities), Stream.of(authority))
                .toArray(Authority[]::new);
        return new JwtParams(userName, userId, memberId, newAuthorities);
    }

    Collection<SimpleGrantedAuthority> grantedAuthorities() {
        return Stream.of(authorities).map(Authority::getValue).map(SimpleGrantedAuthority::new).toList();
    }

    Map<String, Object> toKlabisClaims() {
        Map<String, Object> claims = new HashMap<>();

        Collection<String> authorityStrings = Stream.of(authorities)
                .map(Authority::getValue)
                .toList();

        claims.put(JwtClaimNames.SUB, userName);
        claims.put(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.uuid().toString());
        claims.put(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES, authorityStrings);

        if (memberId != null) {
            claims.put(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, memberId.toString());
        }
        return claims;
    }

}
