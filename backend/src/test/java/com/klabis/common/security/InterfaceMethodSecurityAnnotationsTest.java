package com.klabis.common.security;

import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MethodSecurityAnnotations}, the shared lookup utility that lets
 * {@code @HasAuthority} / {@code @OwnerVisible} be declared on an interface method (e.g. a
 * generated OpenAPI {@code *Api} interface) and still be found when the concrete controller
 * implements that interface — plain {@link Method#getAnnotation} does not search implemented
 * interfaces, since method annotations are not inherited from interfaces in Java.
 */
@DisplayName("MethodSecurityAnnotations interface-aware lookup")
class InterfaceMethodSecurityAnnotationsTest {

    interface AnnotatedOnInterface {
        @HasAuthority(Authority.MEMBERS_READ)
        String secured();

        @OwnerVisible
        String ownerVisible();

        String unsecured();
    }

    @HasAuthority(Authority.MEMBERS_MANAGE)
    interface ClassAnnotatedInterface {
        String inheritedFromInterfaceClass();
    }

    static class ImplementingClass implements AnnotatedOnInterface {
        @Override
        public String secured() {
            return "secured";
        }

        @Override
        public String ownerVisible() {
            return "ownerVisible";
        }

        @Override
        public String unsecured() {
            return "unsecured";
        }
    }

    static class OverridingImplementingClass implements AnnotatedOnInterface {
        @Override
        @HasAuthority(Authority.MEMBERS_MANAGE)
        public String secured() {
            return "secured";
        }

        @Override
        public String ownerVisible() {
            return "ownerVisible";
        }

        @Override
        public String unsecured() {
            return "unsecured";
        }
    }

    static class ClassAnnotatedImplementation implements ClassAnnotatedInterface {
        @Override
        public String inheritedFromInterfaceClass() {
            return "value";
        }
    }

    @Nested
    @DisplayName("method-level annotation on interface")
    class MethodLevelOnInterface {

        @Test
        @DisplayName("finds @HasAuthority declared only on the implemented interface method")
        void findsHasAuthorityFromInterfaceMethod() throws NoSuchMethodException {
            Method method = ImplementingClass.class.getMethod("secured");

            HasAuthority found = MethodSecurityAnnotations.findMethodAnnotation(
                    method, ImplementingClass.class, HasAuthority.class);

            assertThat(found).isNotNull();
            assertThat(found.value()).isEqualTo(Authority.MEMBERS_READ);
        }

        @Test
        @DisplayName("finds @OwnerVisible declared only on the implemented interface method")
        void findsOwnerVisibleFromInterfaceMethod() throws NoSuchMethodException {
            Method method = ImplementingClass.class.getMethod("ownerVisible");

            OwnerVisible found = MethodSecurityAnnotations.findMethodAnnotation(
                    method, ImplementingClass.class, OwnerVisible.class);

            assertThat(found).isNotNull();
        }

        @Test
        @DisplayName("returns null when neither the class method nor any interface method is annotated")
        void returnsNullWhenNotAnnotatedAnywhere() throws NoSuchMethodException {
            Method method = ImplementingClass.class.getMethod("unsecured");

            HasAuthority found = MethodSecurityAnnotations.findMethodAnnotation(
                    method, ImplementingClass.class, HasAuthority.class);

            assertThat(found).isNull();
        }

        @Test
        @DisplayName("concrete class method annotation takes priority over interface method annotation")
        void concreteMethodTakesPriorityOverInterface() throws NoSuchMethodException {
            Method method = OverridingImplementingClass.class.getMethod("secured");

            HasAuthority found = MethodSecurityAnnotations.findMethodAnnotation(
                    method, OverridingImplementingClass.class, HasAuthority.class);

            assertThat(found).isNotNull();
            assertThat(found.value()).isEqualTo(Authority.MEMBERS_MANAGE);
        }
    }

    @Nested
    @DisplayName("class-level annotation on interface")
    class ClassLevelOnInterface {

        @Test
        @DisplayName("finds @HasAuthority declared on the implemented interface type")
        void findsClassLevelAnnotationFromInterface() {
            HasAuthority found = MethodSecurityAnnotations.findClassAnnotation(
                    ClassAnnotatedImplementation.class, HasAuthority.class);

            assertThat(found).isNotNull();
            assertThat(found.value()).isEqualTo(Authority.MEMBERS_MANAGE);
        }

        @Test
        @DisplayName("returns null when no class in the hierarchy or interfaces is annotated")
        void returnsNullWhenNoClassAnnotationAnywhere() {
            HasAuthority found = MethodSecurityAnnotations.findClassAnnotation(
                    ImplementingClass.class, HasAuthority.class);

            assertThat(found).isNull();
        }
    }
}
