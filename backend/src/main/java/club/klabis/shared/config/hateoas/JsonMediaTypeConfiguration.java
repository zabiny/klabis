package club.klabis.shared.config.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.HypermediaMappingInformation;
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
        SimpleModule jsonModule = new SimpleModule("jsonInHateoasModule");
        jsonModule.setMixInAnnotation(RepresentationModel.class, JsonRepresentationModelMixin.class);
        return jsonModule;
    }
}

@JsonMixin(RepresentationModel.class)
@JsonIgnoreProperties("links")
interface JsonRepresentationModelMixin {
}
