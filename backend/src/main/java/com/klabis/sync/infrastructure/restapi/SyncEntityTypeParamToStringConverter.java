package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.core.convert.converter.Converter;

/**
 * The reverse of {@link StringToSyncEntityTypeParamConverter}, used when HATEOAS expands the
 * {@code {entityType}} path variable into a link: without it the constant name ({@code EVENTS})
 * is written instead of the wire value ({@code events}).
 */
@MvcComponent
class SyncEntityTypeParamToStringConverter implements Converter<SyncEntityTypeParam, String> {

    @Override
    public String convert(SyncEntityTypeParam source) {
        return source.getValue();
    }
}
