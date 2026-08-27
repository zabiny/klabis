package com.klabis.common.ui;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.mvc.MvcComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for {@link HalResponseBodyAdvice}: a controller that stores a domain object in
 * {@link HalResponseContext} and then fails must not have its error body wrapped into an
 * {@code EntityModel} carrying that now-stale domain object.
 */
@WebMvcTest(controllers = HalResponseContextLeakTest.LeakTestController.class)
@Import(HalFormsSupport.class)
@DisplayName("HalResponseContext leaking into an error response")
@WithPostprocessors
class HalResponseContextLeakTest {

    @Autowired
    private MockMvc mockMvc;

    record LeakTestResponse(String value) {}

    @MvcComponent
    @RestController
    static class LeakTestController {

        @GetMapping(value = "/api/leak-test/fails-after-context-set", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
        ResponseEntity<LeakTestResponse> failsAfterContextSet() {
            HalResponseContext.setDomain("leaked-domain-object");
            throw new ErrorResponseException(HttpStatus.NOT_FOUND,
                    ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "nothing here"), null);
        }
    }

    @Test
    @WithKlabisMockUser
    @DisplayName("error body stays a plain ProblemDetail, not an EntityModel")
    void errorResponseIsNotWrappedWithStaleDomain() throws Exception {
        mockMvc.perform(get("/api/leak-test/fails-after-context-set").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("nothing here"))
                .andExpect(jsonPath("$._links").doesNotExist());
    }
}
