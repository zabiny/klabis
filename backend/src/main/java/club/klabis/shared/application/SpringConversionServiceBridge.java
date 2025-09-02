package club.klabis.shared.application;

import club.klabis.shared.ConversionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

@Component
class SpringConversionServiceBridge implements ConversionService {

    private final org.springframework.core.convert.ConversionService delegate;

    public SpringConversionServiceBridge(@Qualifier("mvcConversionService") org.springframework.core.convert.ConversionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T, O> O convert(T item, Class<O> result) {
        return delegate.convert(item, result);
    }

    @Override
    public <T, O> O convert(T item, TypeDescriptor typeDesc) {
        return (O) delegate.convert(item, typeDesc);
    }
}
