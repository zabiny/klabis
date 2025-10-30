package club.klabis.users.infrastructure.restapi;

import club.klabis.users.domain.ApplicationUser;
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
public class ApplicationUserIdSerDe {

    public static class Serializer extends JsonSerializer<ApplicationUser.Id> {
        @Override
        public void serialize(ApplicationUser.Id value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            serializers.findValueSerializer(Integer.class).serialize(value.value(), gen, serializers);
        }
    }

    // used to deserialize from URL path and query parameters
    @Component
    public static class FromStringParameterConverter implements Converter<String, ApplicationUser.Id> {

        @Override
        public ApplicationUser.Id convert(String source) {
            if (StringUtils.isBlank(source)) {
                return null;
            } else {
                return new ApplicationUser.Id(Integer.parseInt(source));
            }
        }
    }


    // used to deserialize from URL path and query parameters
    @Component
    public static class ToStringParameterConverter implements Converter<ApplicationUser.Id, String> {

        @Override
        public String convert(ApplicationUser.Id source) {
            return "%d".formatted(source.value());
        }
    }

    // used for request body attributes
    public static class Deserializer extends JsonDeserializer<ApplicationUser.Id> {

        @Override
        public ApplicationUser.Id deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new ApplicationUser.Id(val);
        }

    }

}
