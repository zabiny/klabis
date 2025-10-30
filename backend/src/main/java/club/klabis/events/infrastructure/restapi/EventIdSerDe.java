package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.micrometer.common.util.StringUtils;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@JsonComponent
public class EventIdSerDe {

    public static class Serializer extends JsonSerializer<Event.Id> {
        @Override
        public void serialize(Event.Id value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
    public static class Deserializer extends JsonDeserializer<Event.Id> {

        @Override
        public Event.Id deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new Event.Id(val);
        }

    }

}
