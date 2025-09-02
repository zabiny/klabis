package club.klabis.shared;

import org.springframework.core.convert.TypeDescriptor;

public interface ConversionService {

    <T, O> O convert(T item, Class<O> result);

    <T, O> O convert(T item, TypeDescriptor collection);
}
