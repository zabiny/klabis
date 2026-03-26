package com.klabis.common.patch;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;

import java.util.Objects;

@JacksonComponent
class PatchFieldDeserializer extends StdDeserializer<PatchField<?>> {

    private final Class<?> contentType;

    public PatchFieldDeserializer() {
        this(PatchField.class);
    }

    public PatchFieldDeserializer(Class<?> contentType) {
        super(PatchField.class);
        this.contentType = contentType;
    }

    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws DatabindException {
        Class<?> contentType = Objects.requireNonNull(property.getType()).containedType(0).getRawClass();
        return new PatchFieldDeserializer(contentType);
    }

    @Override
    public PatchField<?> deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return PatchField.of(null);
        }

        Object value = p.readValueAs(contentType);
        return PatchField.of(value);
    }

    @Override
    public PatchField<?> getNullValue(DeserializationContext ctxt) {
        return PatchField.of(null);
    }

    @Override
    public Object getAbsentValue(DeserializationContext ctxt) {
        return PatchField.notProvided();
    }
}
