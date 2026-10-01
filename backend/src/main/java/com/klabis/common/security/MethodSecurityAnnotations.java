package com.klabis.common.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Resolves method/class/parameter-level security annotations ({@code @HasAuthority},
 * {@code @OwnerVisible}, {@code @OwnerId}, {@code @HandleAuthorizationDenied}, ...) across
 * interface boundaries.
 * <p>
 * Java does not inherit method annotations from implemented interfaces: given
 * {@code interface Api { @HasAuthority(X) T method(); }} and
 * {@code class Controller implements Api { public T method() { ... } } }, calling
 * {@code Controller.class.getMethod("method").getAnnotation(HasAuthority.class)} returns
 * {@code null}. Generated OpenAPI controllers ({@code *Api} interfaces implemented by hand-written
 * controllers) rely on exactly this shape, so every security lookup that used to call
 * {@code method.getAnnotation(...)} / {@code type.getAnnotation(...)} directly must instead go
 * through this utility.
 * <p>
 * Priority order (first match wins): the concrete method/class itself, then each interface it
 * implements (declaration order), matched by name and parameter types. This preserves existing
 * behavior — a concrete class/method annotation always overrides one declared on an interface.
 */
public final class MethodSecurityAnnotations {

    private MethodSecurityAnnotations() {
    }

    /**
     * Finds an annotation on the given method, falling back to the same-signature method on any
     * interface implemented by {@code targetClass} (or by the method's declaring class, if
     * {@code targetClass} is {@code null}).
     */
    public static <A extends Annotation> A findMethodAnnotation(Method method, Class<?> targetClass, Class<A> annotationType) {
        A direct = method.getAnnotation(annotationType);
        if (direct != null) {
            return direct;
        }

        Class<?> searchFrom = targetClass != null ? targetClass : method.getDeclaringClass();
        return findOnInterfaces(searchFrom, method.getName(), method.getParameterTypes(), annotationType);
    }

    /**
     * Finds a class-level annotation on {@code targetClass}, falling back to any interface it
     * (transitively) implements.
     */
    public static <A extends Annotation> A findClassAnnotation(Class<?> targetClass, Class<A> annotationType) {
        if (targetClass == null) {
            return null;
        }

        A direct = targetClass.getAnnotation(annotationType);
        if (direct != null) {
            return direct;
        }

        for (Class<?> iface : targetClass.getInterfaces()) {
            A found = findClassAnnotation(iface, annotationType);
            if (found != null) {
                return found;
            }
        }

        Class<?> superclass = targetClass.getSuperclass();
        if (superclass != null) {
            return findClassAnnotation(superclass, annotationType);
        }

        return null;
    }

    /**
     * Finds an annotation on the parameter at {@code parameterIndex}, falling back to the
     * same-signature interface method's parameter at the same index.
     */
    public static <A extends Annotation> A findParameterAnnotation(Method method, Class<?> targetClass, int parameterIndex, Class<A> annotationType) {
        Parameter[] parameters = method.getParameters();
        if (parameterIndex < 0 || parameterIndex >= parameters.length) {
            return null;
        }

        A direct = parameters[parameterIndex].getAnnotation(annotationType);
        if (direct != null) {
            return direct;
        }

        Class<?> searchFrom = targetClass != null ? targetClass : method.getDeclaringClass();
        Method interfaceMethod = findMethodOnInterfaces(searchFrom, method.getName(), method.getParameterTypes());
        if (interfaceMethod == null) {
            return null;
        }
        Parameter[] interfaceParameters = interfaceMethod.getParameters();
        if (parameterIndex >= interfaceParameters.length) {
            return null;
        }
        return interfaceParameters[parameterIndex].getAnnotation(annotationType);
    }

    /**
     * Finds the parameter index annotated with {@code annotationType} on the given method,
     * falling back to the same-signature interface method. Returns -1 if not found anywhere.
     */
    public static int findAnnotatedParameterIndex(Method method, Class<?> targetClass, Class<? extends Annotation> annotationType) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(annotationType)) {
                return i;
            }
        }

        Class<?> searchFrom = targetClass != null ? targetClass : method.getDeclaringClass();
        Method interfaceMethod = findMethodOnInterfaces(searchFrom, method.getName(), method.getParameterTypes());
        if (interfaceMethod == null) {
            return -1;
        }
        Parameter[] interfaceParameters = interfaceMethod.getParameters();
        for (int i = 0; i < interfaceParameters.length; i++) {
            if (interfaceParameters[i].isAnnotationPresent(annotationType)) {
                return i;
            }
        }
        return -1;
    }

    private static <A extends Annotation> A findOnInterfaces(Class<?> type, String methodName, Class<?>[] parameterTypes, Class<A> annotationType) {
        if (type == null) {
            return null;
        }

        for (Class<?> iface : type.getInterfaces()) {
            A found = findOnInterfaceAndItsParents(iface, methodName, parameterTypes, annotationType);
            if (found != null) {
                return found;
            }
        }

        return findOnInterfaces(type.getSuperclass(), methodName, parameterTypes, annotationType);
    }

    /**
     * Matches by exact parameter types, which assumes the interface method and its implementation
     * share one erasure — true for the generated {@code *Api} interfaces (no generic methods) and
     * for every hand-written interface currently carrying security annotations. A generic interface
     * method whose implementation erases differently would be missed here rather than reported.
     */
    private static <A extends Annotation> A findOnInterfaceAndItsParents(Class<?> iface, String methodName, Class<?>[] parameterTypes, Class<A> annotationType) {
        Method candidate = findPublicMethod(iface, methodName, parameterTypes);
        if (candidate != null) {
            A found = candidate.getAnnotation(annotationType);
            if (found != null) {
                return found;
            }
        }

        for (Class<?> parent : iface.getInterfaces()) {
            A found = findOnInterfaceAndItsParents(parent, methodName, parameterTypes, annotationType);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static Method findMethodOnInterfaces(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        if (type == null) {
            return null;
        }

        for (Class<?> iface : type.getInterfaces()) {
            Method found = findMethodOnInterfaceAndItsParents(iface, methodName, parameterTypes);
            if (found != null) {
                return found;
            }
        }

        return findMethodOnInterfaces(type.getSuperclass(), methodName, parameterTypes);
    }

    private static Method findMethodOnInterfaceAndItsParents(Class<?> iface, String methodName, Class<?>[] parameterTypes) {
        Method candidate = findPublicMethod(iface, methodName, parameterTypes);
        if (candidate != null) {
            return candidate;
        }
        for (Class<?> parent : iface.getInterfaces()) {
            Method found = findMethodOnInterfaceAndItsParents(parent, methodName, parameterTypes);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Looks only at methods declared by {@code iface} itself — callers walk the parent interfaces —
     * and avoids {@link Class#getMethod}, whose {@link NoSuchMethodException} per miss is too costly
     * on this proxy-creation hot path.
     */
    private static Method findPublicMethod(Class<?> iface, String methodName, Class<?>[] parameterTypes) {
        for (Method method : iface.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                && !method.isBridge()
                && method.getName().equals(methodName)
                && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }
        return null;
    }
}
