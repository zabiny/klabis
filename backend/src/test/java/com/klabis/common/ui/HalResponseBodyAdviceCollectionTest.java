package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers {@code HalResponseBodyAdvice} wrapping a plain {@code List<DTO>} into a
 * {@code CollectionModel<EntityModel<DTO>>} — the unpaginated counterpart to {@code wrapPage}.
 * Item-level postprocessors must run exactly once per item, and a collection-level processor
 * must be able to decorate the collection itself.
 */
@WebMvcTest(controllers = HalResponseBodyAdviceCollectionTest.CollectionTestController.class)
@Import(HalFormsSupport.class)
@DisplayName("HalResponseBodyAdvice collection (List) handling")
@WithPostprocessors
class HalResponseBodyAdviceCollectionTest {

    @Autowired
    private MockMvc mockMvc;

    record CollectionTestItemResponse(String value) {}

    @MvcComponent
    @RestController
    static class CollectionTestController {

        @GetMapping(value = "/api/collection-test/items", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        ResponseEntity<List<CollectionTestItemResponse>> listItems() {
            List<String> domain = List.of("a", "b");
            List<CollectionTestItemResponse> dtos = domain.stream()
                    .map(CollectionTestItemResponse::new)
                    .toList();

            HalResponseContext.setDomainList(domain);

            return ResponseEntity.ok(dtos);
        }

        @GetMapping(value = "/api/collection-test/unregistered", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        ResponseEntity<List<CollectionTestItemResponse>> listWithoutDomain() {
            return ResponseEntity.ok(List.of(new CollectionTestItemResponse("plain")));
        }
    }

    @MvcComponent
    static class CollectionItemPostprocessor
            extends ModelWithDomainPostprocessor<CollectionTestItemResponse, String> {

        @Override
        public void process(EntityModel<CollectionTestItemResponse> dtoModel, String domain) {
            dtoModel.add(Link.of("/api/collection-test/items/" + domain, IanaLinkRelations.SELF));
        }
    }

    @MvcComponent
    static class CollectionLevelPostprocessor
            implements RepresentationModelProcessor<CollectionModel<EntityModel<CollectionTestItemResponse>>> {

        @Override
        public CollectionModel<EntityModel<CollectionTestItemResponse>> process(
                CollectionModel<EntityModel<CollectionTestItemResponse>> model) {
            model.add(Link.of("/api/collection-test/decorated", "decorated"));
            return model;
        }
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("wraps List<DTO> into _embedded with a self link derived from the request URI")
    void wrapsListIntoCollectionModel() throws Exception {
        mockMvc.perform(get("/api/collection-test/items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$._links.self.href")
                        .value(org.hamcrest.Matchers.containsString("/api/collection-test/items")));
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("pairs each DTO with its domain object so item postprocessors run once per item")
    void runsItemPostprocessorsExactlyOncePerItem() throws Exception {
        mockMvc.perform(get("/api/collection-test/items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.collectionTestItemResponseList", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.collectionTestItemResponseList[0]._links.self.href")
                        .value("/api/collection-test/items/a"))
                .andExpect(jsonPath("$._embedded.collectionTestItemResponseList[1]._links.self.href")
                        .value("/api/collection-test/items/b"));
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("runs collection-level processors against the CollectionModel itself")
    void runsCollectionLevelPostprocessor() throws Exception {
        mockMvc.perform(get("/api/collection-test/items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.decorated.href").value("/api/collection-test/decorated"));
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("leaves a List response without a registered domain list untouched")
    void passesThroughWhenNoDomainRegistered() throws Exception {
        mockMvc.perform(get("/api/collection-test/unregistered").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("plain"));
    }
}
