package com.klabis.common.security.fieldsecurity;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.HasAuthority;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.AuthorizationAdvisorProxyFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Automatically applies {@link AuthorizationAdvisorProxyFactory#proxy(Object)} to response bodies
 * when the body (or its content) implements an interface with {@link PreAuthorize} or
 * {@link HasAuthority} annotated methods.
 * <p>
 * Eliminates the need for controllers to manually call {@code proxyFactory.proxy()} on DTOs.
 * Supports {@link EntityModel}, {@link CollectionModel} (including {@code PagedModel}), and
 * plain objects.
 */
@MvcComponent
@ControllerAdvice
class ResponseBodyFieldAuthorizationAdvice implements ResponseBodyAdvice<Object> {

    private final AuthorizationAdvisorProxyFactory proxyFactory;

    private final ConcurrentMap<Class<?>, Boolean> securityAnnotationCache = new ConcurrentHashMap<>();

    ResponseBodyFieldAuthorizationAdvice(AuthorizationAdvisorProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(
            @Nullable Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        if (body instanceof CollectionModel<?> collectionModel) {
            return applyToCollectionModel(collectionModel);
        }

        if (body instanceof EntityModel<?> entityModel) {
            return applyToEntityModel(entityModel);
        }

        if (hasSecurityAnnotatedInterface(body.getClass())) {
            return proxyFactory.proxy(body);
        }

        return body;
    }

    private CollectionModel<?> applyToCollectionModel(CollectionModel<?> collectionModel) {
        boolean anyProxied = false;
        List<Object> proxiedItems = new ArrayList<>();

        for (Object item : collectionModel.getContent()) {
            if (item instanceof EntityModel<?> entityModel) {
                EntityModel<?> proxied = applyToEntityModel(entityModel);
                proxiedItems.add(proxied);
                anyProxied = anyProxied || (proxied != entityModel);
            } else {
                proxiedItems.add(item);
            }
        }

        if (!anyProxied) {
            return collectionModel;
        }

        @SuppressWarnings("unchecked")
        CollectionModel<Object> result = (CollectionModel<Object>) CollectionModel.of(proxiedItems, collectionModel.getLinks());
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> EntityModel<T> applyToEntityModel(EntityModel<T> entityModel) {
        T content = entityModel.getContent();
        if (content == null || !hasSecurityAnnotatedInterface(content.getClass())) {
            return entityModel;
        }
        T proxied = (T) proxyFactory.proxy(content);
        return EntityModel.of(proxied, entityModel.getLinks());
    }

    private boolean hasSecurityAnnotatedInterface(Class<?> clazz) {
        return securityAnnotationCache.computeIfAbsent(clazz, this::detectSecurityAnnotations);
    }

    private boolean detectSecurityAnnotations(Class<?> clazz) {
        return Arrays.stream(clazz.getInterfaces())
                .anyMatch(iface -> Arrays.stream(iface.getMethods())
                        .anyMatch(this::hasSecurityAnnotation));
    }

    private boolean hasSecurityAnnotation(Method method) {
        return method.isAnnotationPresent(PreAuthorize.class)
                || method.isAnnotationPresent(HasAuthority.class);
    }
}
