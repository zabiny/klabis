package com.klabis;

import com.klabis.common.authorization.TargetId;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.users.HasAuthority;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Every authorization decision goes through {@code AuthorizationEvaluator}: it is the one component that sees
 * the permission snapshot of the request. Code that reads the security annotations or the authorities of the
 * authentication on its own would be a second source of truth that disagrees with the evaluator as soon as a
 * grant is held over a single target.
 *
 * <p>Reading authorities of a {@code UserDetails} (login flow) is not covered: it is not a decision about a
 * request, and the authorization server needs it to issue tokens.
 */
@DisplayName("Authorization architecture")
class AuthorizationArchitectureTest {

    private static final String[] ALLOWED_PACKAGES = {"com.klabis.common.authorization..", "com.klabis.common.security.."};

    private static final Set<Class<?>> EVALUATOR_ONLY_TYPES = Set.of(
            HasAuthority.class, OwnerVisible.class, TargetId.class, ReadAuthority.class,
            OwnershipResolver.class, SecuritySpelEvaluator.class);

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.klabis");
    }

    private static DescribedPredicate<JavaClass> outsideAuthorizationPackages() {
        return not(resideInAnyPackage(ALLOWED_PACKAGES)).as("classes outside common.authorization and common.security");
    }

    @Test
    @DisplayName("authorities of the authentication are read only by common.authorization and common.security")
    void authoritiesOfAuthenticationAreNotReadDirectly() {
        DescribedPredicate<JavaCall<?>> authenticationAuthorities =
                JavaCall.Predicates.target(name("getAuthorities"))
                        .and(JavaCall.Predicates.target(owner(assignableTo(Authentication.class))))
                        .as("Authentication.getAuthorities()");
        DescribedPredicate<JavaCall<?>> tokenHasAuthority =
                JavaCall.Predicates.target(name("hasAuthority"))
                        .and(JavaCall.Predicates.target(owner(assignableTo(KlabisJwtAuthenticationToken.class))))
                        .as("KlabisJwtAuthenticationToken.hasAuthority(..)");

        noClasses().that(outsideAuthorizationPackages())
                .should().callMethodWhere(authenticationAuthorities)
                .orShould().callMethodWhere(tokenHasAuthority)
                .because("authorization questions are answered by AuthorizationEvaluator")
                .check(classes);
    }

    @Test
    @DisplayName("security annotations are read only by common.authorization and common.security")
    void securityAnnotationsAreNotReadDirectly() {
        DescribedPredicate<JavaClass> evaluatorOnlyType = equivalentToAny(EVALUATOR_ONLY_TYPES);

        noClasses().that(outsideAuthorizationPackages())
                .should().callMethodWhere(JavaCall.Predicates.target(owner(evaluatorOnlyType)))
                .orShould().dependOnClassesThat(equivalentTo(OwnershipResolver.class))
                .orShould().dependOnClassesThat(equivalentTo(SecuritySpelEvaluator.class))
                .orShould(referenceClassObjectOf(evaluatorOnlyType))
                .because("annotations are interpreted by AuthorizationEvaluator; applying them is fine, reading them is not")
                .check(classes);
    }

    private static DescribedPredicate<JavaClass> equivalentToAny(Set<Class<?>> types) {
        DescribedPredicate<JavaClass> predicate = DescribedPredicate.alwaysFalse();
        for (Class<?> type : types) {
            predicate = predicate.or(equivalentTo(type));
        }
        return predicate.as("one of " + types.stream().map(Class::getSimpleName).sorted().toList());
    }

    private static com.tngtech.archunit.lang.ArchCondition<JavaClass> referenceClassObjectOf(
            DescribedPredicate<JavaClass> type) {
        return new com.tngtech.archunit.lang.ArchCondition<>("reference the class literal of " + type.getDescription()) {
            @Override
            public void check(JavaClass item, com.tngtech.archunit.lang.ConditionEvents events) {
                item.getReferencedClassObjects().stream()
                        .filter(reference -> type.test(reference.getValue()))
                        .forEach(reference -> events.add(
                                com.tngtech.archunit.lang.SimpleConditionEvent.satisfied(item, item.getName() + " references class literal " + reference.getValue().getName())));
            }
        };
    }
}
