package com.klabis.common.authorization;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.function.SingletonSupplier;

import java.util.UUID;

/**
 * Converts target ids of any type the {@link ConversionService} understands to UUIDs.
 */
final class UuidConversion {

    private final SingletonSupplier<@Nullable ConversionService> conversions;

    UuidConversion(ObjectProvider<ConversionService> conversionService) {
        this.conversions = SingletonSupplier.<ConversionService>ofNullable(() -> conversionService.getIfAvailable());
    }

    @Nullable UUID toUuid(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        ConversionService service = conversions.get();
        if (service == null || !service.canConvert(value.getClass(), UUID.class)) {
            return null;
        }
        try {
            return service.convert(value, UUID.class);
        } catch (ConversionException e) {
            return null;
        }
    }

    boolean canConvert(Class<?> type) {
        ConversionService service = conversions.get();
        return service != null && service.canConvert(type, UUID.class);
    }
}
