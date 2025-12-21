package club.klabis.adapters.api;

import club.klabis.shared.config.security.ApplicationGrant;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Documented
@WithSecurityContext(factory = MockedKlabisUserSecurityContextFactory.class)
public @interface WithMockedKlabisUser {
    int memberId() default -1;

    int applicationUserId() default -1;

    String userName() default "test";

    String firstName() default "User";

    String lastName() default "Tester";

    ApplicationGrant[] applicationGrants() default {};

    int[] canSeeMemberData() default {};
}
