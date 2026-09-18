package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.core.convert.converter.Converter;

/**
 * Binds the {@code {entityType}} path segment to {@link SyncEntityTypeParam} by its wire
 * value ({@code "events"}), not by {@code Enum.valueOf} against the constant name
 * ({@code EVENTS}) that Spring MVC falls back to for an unconverted enum
 * {@code @PathVariable}. {@code @JsonCreator} on {@link SyncEntityTypeParam#fromValue}
 * only applies to JSON body deserialization, never to path variable binding, so without
 * this converter every request — even a valid {@code /api/events/{id}/sync} — fails
 * binding with {@code MethodArgumentTypeMismatchException}.
 * <p>
 * Registered automatically: {@code @MvcComponent} makes {@code MvcConfiguration}'s
 * component scan pick this class up as a Spring bean, and Spring Boot's MVC
 * auto-configuration registers every {@code Converter} bean it finds into the shared
 * {@code mvcConversionService} — no {@code addFormatters} override needed. Deliberately
 * stateless with no constructor dependencies (unlike the {@code SyncStateResponseConverter}
 * incident referenced in its javadoc) so this auto-registration is safe for every
 * {@code @WebMvcTest} slice across the project, not just the sync module's.
 */
@MvcComponent
class StringToSyncEntityTypeParamConverter implements Converter<String, SyncEntityTypeParam> {

    @Override
    public SyncEntityTypeParam convert(String source) {
        return SyncEntityTypeParam.fromValue(source);
    }
}
