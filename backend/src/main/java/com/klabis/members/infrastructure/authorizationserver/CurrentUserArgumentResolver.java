package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import org.springframework.core.MethodParameter;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@MvcComponent
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
               && (parameter.getParameterType().equals(UserId.class) || parameter.getParameterType()
                .equals(CurrentUserData.class));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication authentication = getAuthentication();
        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) authentication;

        Class<?> parameterType = parameter.getParameterType();

        if (parameterType.equals(UserId.class)) {
            return token.getUserId();
        }

        if (parameterType.equals(CurrentUserData.class)) {
            Set<Authority> authorities = token.getAuthorities().stream()
                    .map(a -> Authority.fromString(a.getAuthority()))
                    .collect(Collectors.toSet());
            return new CurrentUserData(token.getUsername(), token.getUserId(), token.getMemberIdUuid().map(MemberId::new).orElse(null), authorities);
        }

        throw new IllegalArgumentException(
                "Unsupported parameter type for @CurrentUser: " + parameterType);
    }

    private Authentication getAuthentication() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("User must be authenticated");
        }

        if (!(authentication instanceof KlabisJwtAuthenticationToken)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Expected JWT authentication, got: " + authentication.getClass().getSimpleName());
        }

        return authentication;
    }
}
