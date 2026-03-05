package com.klabis.common.mvc;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marker for MVC slice beans that should be included in WebMvcTest scans.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MvcComponent {
}
