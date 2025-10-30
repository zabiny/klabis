package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.MemberId;
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
public class MemberIdSerDe {

    public static class Serializer extends JsonSerializer<MemberId> {
        @Override
        public void serialize(MemberId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
    public static class Deserializer extends JsonDeserializer<MemberId> {

        @Override
        public MemberId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            Integer val = ctxt.readValue(p, Integer.class);
            return new MemberId(val);
        }

    }

}
