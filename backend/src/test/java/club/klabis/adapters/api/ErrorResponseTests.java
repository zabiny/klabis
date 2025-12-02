package club.klabis.adapters.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTestConfiguration(controllers = ExampleController.class)
class ErrorResponseTests {

    @Autowired
    MockMvc mockMvc;

    @WithMockUser
    @Test
    void itShouldRespondWithProperlyFormattedErrorFor404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/missingapi").accept("application/json").with(user("someone")))
                .andExpect(status().isNotFound())
                .andDo(print())
                .andExpect(jsonPath("$.type").hasJsonPath())
                .andExpect(jsonPath("$.status").hasJsonPath())
                .andExpect(jsonPath("$.title").hasJsonPath())
                .andExpect(jsonPath("$.detail").hasJsonPath())
                .andExpect(jsonPath("$.instance").hasJsonPath());
    }
}

@RestController
class ExampleController {

}