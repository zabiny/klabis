package club.klabis.adapters.api;

import club.klabis.members.MemberId;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MockedKlabisUserSecurityContextFactory implements WithSecurityContextFactory<WithMockedKlabisUser> {
    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    @Override
    public SecurityContext createSecurityContext(WithMockedKlabisUser annotation) {

        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.applicationGrants())
                .map(ApplicationGrant::name)
                .map(SimpleGrantedAuthority::new)
                .toList();

        KlabisPrincipal principal = new KlabisPrincipal(new ApplicationUser.Id(annotation.applicationUserId()),
                new MemberId(annotation.memberId()),
                annotation.userName(),
                annotation.firstName(),
                annotation.lastName(),
                Arrays.stream(annotation.applicationGrants()).collect(Collectors.toSet()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
