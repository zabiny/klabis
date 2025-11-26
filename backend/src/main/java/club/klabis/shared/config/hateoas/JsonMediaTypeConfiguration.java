package club.klabis.shared.config.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.MediaType;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

@Profile("hateoas")
@Configuration
public class JsonMediaTypeConfiguration implements HypermediaMappingInformation {
    @Override
    public List<MediaType> getMediaTypes() {
        return List.of(MediaType.APPLICATION_JSON);
    }

    @Override
    public JacksonModule getJacksonModule() {
        SimpleModule jsonModule = new SimpleModule("jsonInHateoasModule");
        jsonModule.setMixInAnnotation(RepresentationModel.class, JsonRepresentationModelMixin.class);
        return jsonModule;
    }
}

@JacksonMixin(RepresentationModel.class)
@JsonIgnoreProperties("links")
interface JsonRepresentationModelMixin {
}
