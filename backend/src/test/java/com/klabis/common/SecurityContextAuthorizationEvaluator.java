package com.klabis.common;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionService;

/**
 * Evaluator for plain unit tests that build a component by hand: decides over the snapshot derived from the
 * authentication in the {@code SecurityContextHolder}, with no database and no Spring context.
 */
public final class SecurityContextAuthorizationEvaluator {

    private SecurityContextAuthorizationEvaluator() {
    }

    public static AuthorizationEvaluator create() {
        StaticListableBeanFactory noBeans = new StaticListableBeanFactory();
        return new AuthorizationEvaluator(new FixedAuthorizationSnapshotProvider(),
                noBeans.getBeanProvider(OwnershipResolver.class), noBeans.getBeanProvider(ConversionService.class));
    }
}
