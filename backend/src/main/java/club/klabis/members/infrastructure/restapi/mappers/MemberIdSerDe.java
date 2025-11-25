package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.MemberId;
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
public class MemberIdSerDe {

    public static class Serializer extends ValueSerializer<MemberId> {
        @Override
        public void serialize(MemberId value, JsonGenerator gen, SerializationContext serializers) {
            serializers.findValueSerializer(Integer.class).serialize(value.value(), gen, serializers);
        }
    }

    // used to deserialize from URL path and query parameters
    @Component
    public static class UrlPathParameterConverter implements Converter<String, MemberId> {

        @Override
        public MemberId convert(String source) {
            if (StringUtils.isBlank(source)) {
                return null;
            } else {
                return new MemberId(Integer.parseInt(source));
            }
        }
    }

    // used for request body attributes
    public static class Deserializer extends ValueDeserializer<MemberId> {

        @Override
        public MemberId deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new MemberId(val);
        }

    }

}
