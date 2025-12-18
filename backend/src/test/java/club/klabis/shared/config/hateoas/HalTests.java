package club.klabis.shared.config.hateoas;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.shared.config.restapi.ApiController;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.InputType;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@DisplayName("HAL tests")
@ApiTestConfiguration(controllers = HalTestController.class)
public class HalTests {
    @Autowired
    private MockMvcTester mockMvcTester;

    private MockMvcTester.MockMvcRequestBuilder callSearchApi(String name, Boolean active) {
        return mockMvcTester.get()
                .uri("/search")
                .queryParam("name", name)
                .queryParam("active", active ? "true" : null)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
    }

    @Test
    @Disabled
    @WithMockUser
    @DisplayName("it should handle URL parameters from @ParameterObject argument in _links")
    void itShouldHandleParameterObjectArgument() {
        // many cases will want to use parameter objects - but it seems that HATEOAS doesn't support them - as parameters from them doesn't propagate to links

        assertThat(callSearchApi(null, true))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._links.self.href")
                .isEqualTo("http://localhost/search?active=true");
    }


}


@ExposesResourceFor(HalTestController.DataModel.class)
@ApiController(path = "/search", openApiTagName = "tests")
class HalTestController {

    record Parameters(String name, Boolean active) {
    }

    @GetMapping
    EntityModel<HalTestController.DataModel> getFormDataWithParamObject(@ParameterObject HalTestController.Parameters parameters) {
        EntityModel<HalTestController.DataModel> result = EntityModel.of(new HalTestController.DataModel(1,
                        parameters.name(),
                        parameters.active()))
                .add(WebMvcLinkBuilder.linkTo(methodOn(HalTestController.class).getFormDataWithParamObject(parameters)).withSelfRel());

        return result;

    }

    record DataModel(
            @JsonProperty(access = JsonProperty.Access.READ_WRITE) int id,
            @InputType("userName") String name,

            boolean active
    ) {

    }
}
