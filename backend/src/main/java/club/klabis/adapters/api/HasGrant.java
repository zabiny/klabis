package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationGrant;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@PreAuthorize("hasAuthority('{grant}')")
public @interface HasGrant {
    ApplicationGrant grant();
}
