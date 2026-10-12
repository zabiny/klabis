package com.klabis.common.ui;

import com.klabis.common.CommonInfrastructureWebMvcSetup;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.mvc.MvcComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HalResponseBodyAdviceNestedModelsTest.NestedController.class)
@DisplayName("HalResponseBodyAdvice nested RepresentationModel postprocessing")
@CommonInfrastructureWebMvcSetup
class HalResponseBodyAdviceNestedModelsTest {

    static final List<String> INVOCATIONS = new ArrayList<>();
    static final List<Optional<String>> OWNER_SEEN_BY_PROCESSOR = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    record NestedItem(String name) {}

    record Owner(String id) {}

    record NestedPayload(String title, List<EntityModel<NestedItem>> items) {}

    @MvcComponent
    @RestController
    static class NestedController {

        @GetMapping(value = "/api/nested-test", produces = {MediaTypes.HAL_FORMS_JSON_VALUE, "application/json"})
        ResponseEntity<NestedPayload> nested() {
            HalResponseContext.setDomain("domain");
            HalResponseContext.setContext(new Owner("owner-1"));
            return ResponseEntity.ok(new NestedPayload("t",
                    List.of(EntityModel.of(new NestedItem("a")), EntityModel.of(new NestedItem("b")))));
        }
    }

    @MvcComponent
    static class NestedItemProcessor implements RepresentationModelProcessor<EntityModel<NestedItem>> {
        @Override
        public EntityModel<NestedItem> process(EntityModel<NestedItem> model) {
            INVOCATIONS.add(model.getContent().name());
            OWNER_SEEN_BY_PROCESSOR.add(HalResponseContext.findContext(Owner.class).map(Owner::id));
            model.add(Link.of("/items/" + model.getContent().name(), "self"));
            return model;
        }
    }

    @MvcComponent
    static class NestedPayloadProcessor implements RepresentationModelProcessor<EntityModel<NestedPayload>> {
        @Override
        public EntityModel<NestedPayload> process(EntityModel<NestedPayload> model) {
            model.add(Link.of("/nested", "self"));
            return model;
        }
    }

    @BeforeEach
    void reset() {
        INVOCATIONS.clear();
        OWNER_SEEN_BY_PROCESSOR.clear();
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("HAL response runs the item postprocessor exactly once per nested item, with request context available")
    void halRunsItemProcessorOncePerItem() throws Exception {
        mockMvc.perform(get("/api/nested-test").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("/nested"))
                .andExpect(jsonPath("$.items[0]._links.self.href").value("/items/a"))
                .andExpect(jsonPath("$.items[1]._links.self.href").value("/items/b"));

        assertThat(INVOCATIONS).containsExactly("a", "b");
        assertThat(OWNER_SEEN_BY_PROCESSOR).containsExactly(Optional.of("owner-1"), Optional.of("owner-1"));
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("plain JSON response performs no postprocessing of nested items")
    void plainJsonDoesNotInvokeItemProcessor() throws Exception {
        mockMvc.perform(get("/api/nested-test").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]._links").doesNotExist());

        assertThat(INVOCATIONS).isEmpty();
    }
}
