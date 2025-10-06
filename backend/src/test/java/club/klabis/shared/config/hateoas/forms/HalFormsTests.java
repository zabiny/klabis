package club.klabis.shared.config.hateoas.forms;

import club.klabis.shared.config.restapi.ApiController;
import club.klabis.tests.common.MapperTestConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HAL+FORMS tests")
@Import({MapperTestConfiguration.class})
@WebMvcTest(controllers = TestController.class)
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL_FORMS)
public class HalFormsTests {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("affordBetter tests")
    @Nested
    class AffordBetterTests {

        @DisplayName("it should return HAL+FORM template properties in expected order")
        @Test
        @WithMockUser
        void itShouldReturnTemplatePropertiesInExpectedOrder() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/formsTest").accept(MediaTypes.HAL_FORMS_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates['default'].properties[*].name",
                            Matchers.contains("id", "name", "address")));
        }
    }

}

@ExposesResourceFor(TestController.DataModel.class)
@ApiController(path = "/formsTest", openApiTagName = "tests")
class TestController {

    @GetMapping
    EntityModel<DataModel> getFormData() {
        EntityModel<DataModel> result = EntityModel.of(new DataModel(1, "name", "surname"))
                .add(WebMvcLinkBuilder.linkTo(methodOn(this.getClass()).getFormData())
                        .withSelfRel()
                        .andAffordance(affordBetter(methodOn(getClass()).putFormData(null)))
                        .withRel("update"));

        return result;

    }

    @PutMapping
    ResponseEntity<Void> putFormData(@RequestBody DataModel data) {
        return ResponseEntity.created(URI.create("/formData/1")).build();
    }

    record DataModel(int id, String name, String address) {

    }
}


