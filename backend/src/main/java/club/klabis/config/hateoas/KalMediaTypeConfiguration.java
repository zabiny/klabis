package club.klabis.config.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

@Profile("hateoas")
@Configuration
public class KalMediaTypeConfiguration implements HypermediaMappingInformation {
    @Override
    public List<MediaType> getMediaTypes() {
        return List.of(MediaType.valueOf("application/kal+json"));
    }

    @Override
    public Module getJacksonModule() {
        Jackson2HalModule halJacksonModule = new Jackson2HalModule();
        halJacksonModule.setMixInAnnotation(RepresentationModel.class, KalRepresentationModelMixin.class);
        return halJacksonModule;
    }
}

@JsonMixin(RepresentationModel.class)
interface KalRepresentationModelMixin {
    @JsonProperty("_actions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(
            using = KalLinksSerializer.class
    )
    public Links getLinks();
}

class KalLinksSerializer extends JsonSerializer<Links> {

    //    @Override
    public void serialize(Link link, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(link.getRel().value());
    }

    @Override
    public void serialize(Links links, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray();
        List<Link> actionLinks = links.stream().filter(i -> !i.getRel().isSameAs(LinkRelation.of("self"))).toList();
        for (Link l : actionLinks) {
            serialize(l, jsonGenerator, serializerProvider);
        }
        jsonGenerator.writeEndArray();
    }
}
