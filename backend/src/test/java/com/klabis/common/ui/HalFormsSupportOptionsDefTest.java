package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers HalFormsOptionsDef (D3 in app-review-fixes-2026-09/design.md): klabisAffordWithOptions
 * must render an Inline-wrapped option exactly as the previous plain-List signature did, and a
 * Remote-wrapped option as a HAL-FORMS options.link pointing at the given href.
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({OptionsDefExampleController.class})
class HalFormsSupportOptionsDefTest {

    @Autowired
    MockMvc mockMvc;

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Inline-wrapped option renders as options.inline, same as a plain inline list")
    void inlineOptionRendersAsInlineOptions() throws Exception {
        mockMvc.perform(get("/api/testOptionsDef/inline")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.inline").isArray())
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.inline[0].value").value("1"))
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.inline[0].prompt").value("Elite"))
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.link").doesNotExist());
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Remote-wrapped option renders as options.link pointing at the given href")
    void remoteOptionRendersAsOptionsLink() throws Exception {
        mockMvc.perform(get("/api/testOptionsDef/remote")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.link.href").value("/api/testOptionsDef/categories"))
                .andExpect(jsonPath("$._templates.edit.properties[?(@.name == 'category')].options.inline").doesNotExist());
    }
}

record OptionsDefEditRequest(String category) {
}

record OptionsDefDummy(String value) {
}

@RestController
@RequestMapping("/api/testOptionsDef")
class OptionsDefExampleController {

    @GetMapping(value = "/inline", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<OptionsDefDummy>> getWithInlineOptions() {
        EntityModel<OptionsDefDummy> model = EntityModel.of(new OptionsDefDummy("dummy"));
        klabisLinkTo(methodOn(OptionsDefExampleController.class).getWithInlineOptions()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(OptionsDefExampleController.class).edit(null),
                                Map.of("category", new HalFormsOptionsDef.Inline(
                                        List.of(new HalFormsInlineOption("1", "Elite"))))))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/remote", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<OptionsDefDummy>> getWithRemoteOptions() {
        EntityModel<OptionsDefDummy> model = EntityModel.of(new OptionsDefDummy("dummy"));
        klabisLinkTo(methodOn(OptionsDefExampleController.class).getWithRemoteOptions()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(OptionsDefExampleController.class).edit(null),
                                Map.of("category", new HalFormsOptionsDef.Remote(
                                        org.springframework.hateoas.Link.of("/api/testOptionsDef/categories")))))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/categories", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<List<String>> listCategories() {
        return ResponseEntity.ok(List.of("Elite"));
    }

    @PutMapping(value = "/edit", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> edit(@RequestBody OptionsDefEditRequest request) {
        return ResponseEntity.noContent().build();
    }
}
