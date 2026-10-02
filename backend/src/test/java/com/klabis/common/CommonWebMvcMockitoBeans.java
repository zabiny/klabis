package com.klabis.common;

import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.common.users.application.PasswordChangePort;
import com.klabis.common.users.application.PasswordSetupService;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the shared-kernel services required by common web beans (password and permission controllers,
 * resource-server account status filter, authorization server components) and by web beans of other
 * modules ({@link OrisClubKeyPort}).
 * <p>
 * {@link UserService} and {@link UserDetailsService} are needed in every {@code @WebMvcTest} that loads
 * the security filter chain. {@link UserDetailsService} is mocked because it is framework security
 * infrastructure backed by persistence, not a primary port of any adapter under test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        OrisClubKeyPort.class,
        PasswordChangePort.class,
        PasswordSetupService.class,
        PermissionService.class,
        UserService.class,
        UserDetailsService.class
})
public @interface CommonWebMvcMockitoBeans {
}
