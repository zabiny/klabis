package com.klabis.common.patch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.Objects;

@JsonComponent
class PatchFieldDeserializer extends StdDeserializer<PatchField<?>> implements ContextualDeserializer {

    private final Class<?> contentType;

    public PatchFieldDeserializer() {
        this(null);
    }

    public PatchFieldDeserializer(Class<?> contentType) {
        super(PatchField.class);
        this.contentType = contentType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        Class<?> contentType = Objects.requireNonNull(property.getType()).containedType(0).getRawClass();
        return new PatchFieldDeserializer(contentType);
    }

    @Override
    public PatchField<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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
