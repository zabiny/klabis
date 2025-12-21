package org.springframework.hateoas;

import club.klabis.shared.config.security.HasGrant;
import club.klabis.shared.config.security.HasMemberGrant;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

// TODO: remove dependency on "Klabis specific" code (= add some kinds of annotation resolvers which can be registered through configuration and move KLabisSpecific annotation handling to Klabis code)
class SecurityUtils {

    private static final DefaultMethodSecurityExpressionHandler securityExpressionHandler = new DefaultMethodSecurityExpressionHandler();
    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);


    public static boolean isAuthorizedToCall(@NonNull SimpleMethodInvocation method) {
        boolean result = true;

        PreAuthorize preAuthorizeAnnotation = method.getMethod().getAnnotation(PreAuthorize.class);
        if (preAuthorizeAnnotation != null) {
            result &= evaluateSpringSecurityExpression(preAuthorizeAnnotation, method);
        }

        PostAuthorize postAuthorizeAnnotation = method.getMethod().getAnnotation(PostAuthorize.class);
        if (postAuthorizeAnnotation != null) {
            // if it would be needed, we would have to handle such affordance in some "post" step (HTTP filter, MessageConverter like HATEOAS, etc.. )
            throw new IllegalArgumentException(
                    "@PostAuthorize annotation authorization is NOT supported - it requires actual return value what we can't provide in this case");
        }

        Secured securedAnnotation = method.getMethod().getAnnotation(Secured.class);
        if (securedAnnotation != null) {
            throw new IllegalArgumentException("@Secured annotation authorization is NOT supported");
        }

        HasGrant hasGrantAnnotation = method.getMethod().getAnnotation(HasGrant.class);
        if (hasGrantAnnotation != null) {
            result &= evaluateSpringSecurityExpression(hasGrantAnnotation, method);
        }

        HasMemberGrant hasMemberGrantAnnotation = method.getMethod().getAnnotation(HasMemberGrant.class);
        if (hasMemberGrantAnnotation != null) {
            result &= evaluateSpringSecurityExpression(hasMemberGrantAnnotation, method);
        }
        return result;
    }

    private static boolean evaluateSpringSecurityExpression(HasMemberGrant hasMemberGrantAnnotation, @NonNull SimpleMethodInvocation method) {
        return evaluateSpringSecurityExpression("@klabisAuthorizationService.canEditMemberData(%s)".formatted(
                hasMemberGrantAnnotation.memberId()), method);
    }

    private static boolean evaluateSpringSecurityExpression(HasGrant annotation, SimpleMethodInvocation s) {
        // must match to whatever is in @HasGrant annotation
        return evaluateSpringSecurityExpression("hasAuthority('%s')".formatted(annotation.value()), s);
    }

    private static boolean evaluateSpringSecurityExpression(PreAuthorize annotation, SimpleMethodInvocation s) {
        return evaluateSpringSecurityExpression(annotation.value(), s);
    }

    private static ApplicationContext applicationContext;

    private static boolean evaluateSpringSecurityExpression(String expression, SimpleMethodInvocation s) {
        StandardEvaluationContext context = createSpELEvaluationContextForSecurityExpression(s);

        SpelExpressionParser parser = new SpelExpressionParser();
        Expression parsedExpression = parser.parseExpression(expression);
        try {
            Object result = parsedExpression.getValue(context);

            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception ex) {
            LOG.warn("Failed to evaluate spring security expression {} -> returning NOT_AUTHORIZED. Error {}",
                    expression,
                    ex.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception: ", ex);
            }
            return false;
        }
    }

    private static @NonNull StandardEvaluationContext createSpELEvaluationContextForSecurityExpression(SimpleMethodInvocation s) {
        // Získáme aktuální kontext bezpečnosti
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        StandardEvaluationContext context = (StandardEvaluationContext) securityExpressionHandler.createEvaluationContext(
                authentication,
                s);

        if (applicationContext != null) {
            context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        } else {
            LOG.debug("Application context is not set in SecurityUtils!");
        }
        return context;
    }


    static void setApplicationContext(ApplicationContext applicationContext) {
        SecurityUtils.applicationContext = applicationContext;
    }
}
