package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for {@link HalResponseBodyAdvice#wrapPage}: when a controller's self-link
 * supplier resolves to {@link Optional#empty()} (target method not authorized for the current
 * user), the assembler-provided self link must be left untouched — not overwritten with {@code null}.
 * <p>
 * {@code klabisLinkTo} returns {@code Optional.empty()} when the caller lacks authority for the
 * linked method. Before the fix, the supplier collapsed that to a bare {@code null} via
 * {@code orElse(null)}, and {@code PagedModel.mapLink} has no null check — it unconditionally
 * removes the old link and adds whatever the mapping function returns, corrupting {@code _links.self}.
 */
@WebMvcTest(controllers = HalResponseBodyAdviceTest.SelfLinkTestController.class)
@Import(HalFormsSupport.class)
@DisplayName("HalResponseBodyAdvice self-link supplier handling")
@WithPostprocessors
class HalResponseBodyAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    record TestItemResponse(String value) {}

    @MvcComponent
    @RestController
    static class SelfLinkTestController {

        @GetMapping(value = "/api/self-link-test/items", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        ResponseEntity<Page<TestItemResponse>> listItems(@PageableDefault Pageable pageable) {
            List<String> domain = List.of("a", "b");
            Page<TestItemResponse> page = new PageImpl<>(
                    domain.stream().map(TestItemResponse::new).toList(),
                    PageRequest.of(0, 10), domain.size());

            HalResponseContext.setDomainList(domain);
            HalResponseContext.setSelfLinkSupplier(() ->
                    klabisLinkTo(methodOn(SelfLinkTestController.class).securedTarget())
                            .map(builder -> builder.withSelfRel()));

            return ResponseEntity.ok(page);
        }

        @GetMapping(value = "/api/self-link-test/secured", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        @HasAuthority(Authority.MEMBERS_MANAGE)
        ResponseEntity<Void> securedTarget() {
            return ResponseEntity.noContent().build();
        }
    }

    @Nested
    @DisplayName("self-link supplier target requires an authority the caller lacks")
    class SupplierResolvesToEmpty {

        @Test
        @WithKlabisMockUser(username = "noAuthUser")
        @DisplayName("assembler-provided self link is kept, not overwritten with null")
        void keepsAssemblerSelfLinkWhenSupplierIsEmpty() throws Exception {
            mockMvc.perform(get("/api/self-link-test/items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(
                            org.hamcrest.Matchers.containsString("/api/self-link-test/items")));
        }
    }

    @Nested
    @DisplayName("self-link supplier target is authorized")
    class SupplierResolvesToPresent {

        @Test
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        @DisplayName("self link is overwritten with the supplier's link")
        void overwritesSelfLinkWhenSupplierIsPresent() throws Exception {
            mockMvc.perform(get("/api/self-link-test/items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(
                            org.hamcrest.Matchers.containsString("/api/self-link-test/secured")));
        }
    }
}
