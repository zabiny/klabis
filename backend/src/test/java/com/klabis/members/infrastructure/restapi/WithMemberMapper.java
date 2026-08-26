package com.klabis.members.infrastructure.restapi;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides {@link MemberMapper} for {@code @WebMvcTest} slices that don't otherwise scan the
 * {@code members.infrastructure.restapi} package. Needed because {@link MembersExceptionHandler}
 * is a global {@code @RestControllerAdvice} picked up by every {@code @WebMvcTest}, and it depends
 * on {@link MemberMapper}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MemberMapperImpl.class)
public @interface WithMemberMapper {
}
