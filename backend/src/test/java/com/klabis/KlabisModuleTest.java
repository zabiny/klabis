package com.klabis;

import org.springframework.core.annotation.AliasFor;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ApplicationModuleTest} with the project's test defaults.
 * <p>
 * Automatic module verification is off: it runs a full ArchUnit import for every test class, while
 * {@code ModuleStructureVerificationTest} verifies the module structure once.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApplicationModuleTest(verifyAutomatically = false)
@ActiveProfiles("test")
public @interface KlabisModuleTest {

    @AliasFor(annotation = ApplicationModuleTest.class, attribute = "mode")
    BootstrapMode mode() default BootstrapMode.STANDALONE;

    @AliasFor(annotation = ApplicationModuleTest.class, attribute = "module")
    String module() default "";

    @AliasFor(annotation = ApplicationModuleTest.class, attribute = "extraIncludes")
    String[] extraIncludes() default {};
}
