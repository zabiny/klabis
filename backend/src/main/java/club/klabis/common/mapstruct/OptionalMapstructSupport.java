package club.klabis.common.mapstruct;

import org.mapstruct.Condition;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface OptionalMapstructSupport {
// Wrap doesn't make sense - that would mean that Optional is used as argument of setter... and that shouldn't happen
//    default <T> Optional<T> wrap(T value) {
//        return Optional.ofNullable(value);
//    }

    default <T> T unwrap(Optional<T> value) {
        return value.orElse(null);
    }

    @Condition
    default boolean hasValue(Optional<?> value) {
        return value.isPresent();
    }

}
