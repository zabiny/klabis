package club.klabis.shared.config.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.MediaType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

@Profile("hateoas")
@Configuration
public class KalMediaTypeConfiguration implements HypermediaMappingInformation {

    public static final MediaType KAL_MEDIA_TYPE = MediaType.valueOf("application/klabis+json");

    @Override
    public List<MediaType> getMediaTypes() {
        return List.of(KAL_MEDIA_TYPE);
    }

    @Override
    public JacksonModule getJacksonModule() {
        SimpleModule klabisMediaTypeJacksonModule = new SimpleModule();
        klabisMediaTypeJacksonModule.setMixInAnnotation(RepresentationModel.class, KalRepresentationModelMixin.class);
        return klabisMediaTypeJacksonModule;
    }
}

@JacksonMixin(RepresentationModel.class)
interface KalRepresentationModelMixin {
    @JsonProperty("_actions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(
            using = KalLinksSerializer.class
    )
    public Links getLinks();
}

class KalLinksSerializer extends ValueSerializer<Links> {

    @Override
    public boolean isEmpty(SerializationContext provider, Links value) {
        return getActionLinks(value).isEmpty();
    }

    //    @Override
    public void serialize(Link link, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        jsonGenerator.writeString(link.getRel().value());
    }

    private List<Link> getActionLinks(Links links) {
        return links.stream().filter(i -> !i.getRel().isSameAs(LinkRelation.of("self"))).toList();
    }

    @Override
    public void serialize(Links links, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        jsonGenerator.writeStartArray();
        for (Link l : getActionLinks(links)) {
            serialize(l, jsonGenerator, serializerProvider);
        }
        jsonGenerator.writeEndArray();
    }
}
