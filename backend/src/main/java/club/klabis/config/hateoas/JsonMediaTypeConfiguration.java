package club.klabis.config.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.Module;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;

import java.util.List;

@Profile("hateoas")
@Configuration
public class JsonMediaTypeConfiguration implements HypermediaMappingInformation {
    @Override
    public List<MediaType> getMediaTypes() {
        return List.of(MediaType.APPLICATION_JSON);
    }

    @Override
    public Module getJacksonModule() {
        Jackson2HalModule halJacksonModule = new Jackson2HalModule();
        halJacksonModule.setMixInAnnotation(RepresentationModel.class, JsonRepresentationModelMixin.class);
        return halJacksonModule;
    }
}

@JsonMixin(RepresentationModel.class)
@JsonIgnoreProperties("links")
interface JsonRepresentationModelMixin {
}