package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for {@link HalResponseBodyAdvice#wrapPage}: the collection-level self link is
 * derived directly from the current request's URI and query string via {@code ServerHttpRequest},
 * not re-built via {@code klabisLinkTo(methodOn(...))}. The controller method already ran (and
 * passed authorization) for exactly that URI, so no separate authorization check applies to the
 * self link itself — only affordances to *other* endpoints (added by a postprocessor, see
 * {@code MemberListPostprocessor}) still go through {@code klabisAfford} and stay
 * authorization-sensitive.
 */
@WebMvcTest(controllers = HalResponseBodyAdviceTest.SelfLinkTestController.class)
@Import(HalFormsSupport.class)
@DisplayName("HalResponseBodyAdvice self link handling for paged responses")
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
                    pageable, domain.size());

            HalResponseContext.setDomainList(domain);

            return ResponseEntity.ok(page);
        }
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("self link reflects the actual request URI, including query parameters not known to PagedResourcesAssembler")
    void selfLinkReflectsActualRequestUri() throws Exception {
        mockMvc.perform(get("/api/self-link-test/items").param("size", "10").param("extra", "keep-me")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("/api/self-link-test/items"),
                                org.hamcrest.Matchers.containsString("extra=keep-me"))));
    }
}
