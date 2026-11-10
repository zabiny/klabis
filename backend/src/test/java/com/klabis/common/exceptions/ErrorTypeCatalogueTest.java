package com.klabis.common.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.objenesis.ObjenesisStd;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Error type catalogue")
class ErrorTypeCatalogueTest {

    private static final Path PROBLEM_SPEC = Path.of("..", "docs", "openapi", "spec", "_shared", "problem.yaml");

    @Test
    @DisplayName("every emitted type is distinct, listed in the spec ErrorType enum, and every enum value is emitted")
    void emittedTypesMatchSpecEnum() throws Exception {
        List<String> emitted = emittedTypes();

        assertThat(emitted).doesNotHaveDuplicates();
        assertThat(specEnum()).containsExactlyInAnyOrderElementsOf(emitted);
    }

    @Test
    @DisplayName("every emitted type is a /errors/ relative URI in kebab-case")
    void emittedTypesAreWellFormed() throws Exception {
        assertThat(emittedTypes()).allMatch(type -> type.matches("/errors/[a-z0-9]+(-[a-z0-9]+)*"));
    }

    @Test
    @DisplayName("every concrete subclass of the five common bases is a UserFacingError")
    void everyBaseSubclassIsUserFacing() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        for (Class<?> base : List.of(BusinessRuleViolationException.class, ConflictException.class,
                ResourceNotFoundException.class, InvalidDataException.class, AuthorizationException.class)) {
            scanner.addIncludeFilter(new AssignableTypeFilter(base));
        }
        List<String> untyped = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.klabis")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            if (!isTestClass(type) && !UserFacingError.class.isAssignableFrom(type)) {
                untyped.add(type.getName());
            }
        }

        assertThat(untyped).isEmpty();
    }

    private List<String> emittedTypes() throws Exception {
        List<String> emitted = new ArrayList<>(CommonErrorTypes.ALL);
        for (Class<?> errorClass : userFacingErrorClasses()) {
            UserFacingError instance = (UserFacingError) new ObjenesisStd().newInstance(errorClass);
            instance.errorTypes().stream().map(CommonErrorTypes::uri).forEach(emitted::add);
        }
        return emitted;
    }

    private static List<Class<?>> userFacingErrorClasses() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(UserFacingError.class));
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.klabis")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            if (!isTestClass(type)) {
                classes.add(type);
            }
        }
        return classes;
    }

    private static boolean isTestClass(Class<?> type) {
        String location = type.getProtectionDomain().getCodeSource().getLocation().getPath();
        return location.contains("/classes/java/test/");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> specEnum() throws IOException {
        try (InputStream in = Files.newInputStream(PROBLEM_SPEC)) {
            Map<String, Object> spec = new Yaml().load(in);
            var schemas = (Map<String, Object>) ((Map<String, Object>) spec.get("components")).get("schemas");
            var errorType = (Map<String, Object>) schemas.get("ErrorType");
            return new TreeSet<>((List<String>) errorType.get("enum"));
        }
    }
}
