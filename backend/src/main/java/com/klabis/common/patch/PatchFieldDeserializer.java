package com.klabis.common.patch;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;

import java.util.Objects;

@JacksonComponent
class PatchFieldDeserializer extends StdDeserializer<PatchField<?>> {

    private final JavaType contentType;

    public PatchFieldDeserializer() {
        super(PatchField.class);
        this.contentType = null;
    }

    private PatchFieldDeserializer(JavaType contentType) {
        super(PatchField.class);
        this.contentType = contentType;
    }

    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws DatabindException {
        JavaType contentType = Objects.requireNonNull(property.getType()).containedType(0);
        return new PatchFieldDeserializer(contentType);
    }

    @Override
    public PatchField<?> deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return PatchField.of(null);
        }

        Object value = contentType != null
                ? p.readValueAs(contentType)
                : p.readValueAs(Object.class);
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
