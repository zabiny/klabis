package org.springframework.hateoas;

import club.klabis.shared.config.security.HasGrant;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.hateoas.server.core.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

class SecurityUtils {

    private static final DefaultMethodSecurityExpressionHandler securityExpressionHandler = new DefaultMethodSecurityExpressionHandler();


    public static boolean isAuthorizedToCall(MethodInvocation method) {
        SimpleMethodInvocation s = new SimpleMethodInvocation(null, method.getMethod(), method.getArguments());

        boolean result = true;

        PreAuthorize preAuthorizeAnnotation = method.getMethod().getAnnotation(PreAuthorize.class);
        if (preAuthorizeAnnotation != null) {
            result &= evaluateSpringSecurityExpression(preAuthorizeAnnotation, s);
        }

        HasGrant hasGrantAnnotation = method.getMethod().getAnnotation(HasGrant.class);
        if (hasGrantAnnotation != null) {
            result &= evaluateSpringSecurityExpression(hasGrantAnnotation, s);
        }
        return result;
    }

    private static boolean evaluateSpringSecurityExpression(HasGrant annotation, SimpleMethodInvocation s) {
        // must match to whatever is in @HasGrant annotation
        return evaluateSpringSecurityExpression("hasAuthority('%s')".formatted(annotation.value()), s);
    }

    private static boolean evaluateSpringSecurityExpression(PreAuthorize annotation, SimpleMethodInvocation s) {
        return evaluateSpringSecurityExpression(annotation.value(), s);
    }

    private static boolean evaluateSpringSecurityExpression(String expression, SimpleMethodInvocation s) {
        // Získáme aktuální kontext bezpečnosti
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        EvaluationContext context = securityExpressionHandler.createEvaluationContext(authentication, s);

        // Vytvoříme SpEL parser a vyhodnotíme výraz
        SpelExpressionParser parser = new SpelExpressionParser();
        Expression parsedExpression = parser.parseExpression(expression);

        // Vyhodnotíme výraz a vrátíme výsledek jako boolean
        Object result = parsedExpression.getValue(context);

        return result instanceof Boolean ? (Boolean) result : false;
    }


}
