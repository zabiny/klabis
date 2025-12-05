package club.klabis.users.infrastructure.restapi;

import club.klabis.shared.config.security.ApplicationGrant;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
public class ApplicationGrantSerDe {

    public static class ApplicationGrantJacksonSerializer extends ValueSerializer<ApplicationGrant> {

        @Override
        public void serialize(ApplicationGrant value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(value.getGrantName());
        }
    }

    public static class ApplicationGrantJacksonDeserializer extends ValueDeserializer<ApplicationGrant> {

        @Override
        public ApplicationGrant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            String grantName = p.getString();
            return ApplicationGrant.fromGrantName(grantName);
        }
    }

}
