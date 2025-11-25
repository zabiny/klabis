package club.klabis.users.infrastructure.restapi;

import club.klabis.users.domain.ApplicationUser;
import io.micrometer.common.util.StringUtils;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JsonComponent
public class ApplicationUserIdSerDe {

    public static class Serializer extends ValueSerializer<ApplicationUser.Id> {
        @Override
        public void serialize(ApplicationUser.Id value, JsonGenerator gen, SerializationContext serializers) {
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
    public static class Deserializer extends ValueDeserializer<ApplicationUser.Id> {

        @Override
        public ApplicationUser.Id deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new ApplicationUser.Id(val);
        }

    }

}
