package com.klabis.common.ui;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers inline HAL-FORMS options for every enum-typed property of every request DTO record,
 * directly with Spring HATEOAS via {@link HalFormsConfiguration#withOptions}, keyed by the exact
 * {@code (payload Class, property name)} pair. This is deliberately not routed through
 * {@code HalFormsSupport}'s request-attribute options mechanism, which keys by property name only
 * and would let one affordance's options leak into another affordance's same-named property in the
 * same response. Explicit per-request options (e.g. permission or sync-adapter filtered subsets)
 * still take priority — see {@code HalFormsMultiPropertyModule.OptionsPropertyWriter}.
 */
@MvcComponent
class EnumOptionsAutoConfiguration {

    /** Generated and hand-written request/response DTOs alike live directly in this package suffix. */
    private static final String SCAN_BASE_PACKAGE = "com.klabis";
    private static final String REST_API_PACKAGE_SUFFIX = ".infrastructure.restapi";

    @Bean
    MediaTypeConfigurationCustomizer<HalFormsConfiguration> autoEnumHalFormsOptionsCustomizer() {
        return configuration -> {
            HalFormsConfiguration result = configuration;
            for (Class<?> dtoType : findRestApiRecordTypes()) {
                for (Map.Entry<String, List<String>> entry : EnumOptionsSupport.enumOptionsByProperty(dtoType).entrySet()) {
                    List<String> values = entry.getValue();
                    result = result.withOptions(dtoType, entry.getKey(),
                            metadata -> HalFormsOptions.inline(values.toArray()));
                }
            }
            return result;
        };
    }

    private static Set<Class<?>> findRestApiRecordTypes() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) ->
                "java.lang.Record".equals(metadataReader.getClassMetadata().getSuperClassName())
                        && metadataReader.getClassMetadata().getClassName().contains(REST_API_PACKAGE_SUFFIX + "."));

        Set<Class<?>> result = new LinkedHashSet<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(SCAN_BASE_PACKAGE)) {
            String className = candidate.getBeanClassName();
            if (className == null) {
                continue;
            }
            try {
                Class<?> type = Class.forName(className, false, EnumOptionsAutoConfiguration.class.getClassLoader());
                if (type.isRecord()) {
                    result.add(type);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Ignore: a record this JVM can't load isn't a request DTO we can serve anyway.
            }
        }
        return result;
    }
}
