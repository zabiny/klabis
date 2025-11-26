package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import io.micrometer.common.util.StringUtils;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
public class EventIdSerDe {

    public static class Serializer extends ValueSerializer<Event.Id> {
        @Override
        public void serialize(Event.Id value, JsonGenerator gen, SerializationContext serializers) {
            serializers.findValueSerializer(Integer.class).serialize(value.value(), gen, serializers);
        }
    }

    // used to deserialize from URL path and query parameters
    @Component
    public static class UrlPathParameterConverter implements Converter<String, Event.Id> {

        @Override
        public Event.Id convert(String source) {
            if (StringUtils.isBlank(source)) {
                return null;
            } else {
                return new Event.Id(Integer.parseInt(source));
            }
        }
    }

    // used for request body attributes
    public static class Deserializer extends ValueDeserializer<Event.Id> {

        @Override
        public Event.Id deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new Event.Id(val);
        }

    }

}
