package com.klabis.members.application;

import com.klabis.TestApplicationConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.test.ApplicationModuleTest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("Member Registration Integration Tests")
class MemberRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ADMIN_USERNAME = "admin";

    @Test
    @DisplayName("Registration of member should succeed")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldCompleteRegistrationFlowForAdult() throws Exception {
        // When: Registering the member
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "1999-01-15",
                                            "nationality": "CZ",
                                            "gender": "MALE",
                                            "email": "jan.novak@example.com",
                                            "phone": "+420777123456",
                                            "address": {
                                                "street": "Hlavní 123",
                                                "city": "Praha",
                                                "postalCode": "11000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, Matchers.containsString("/api/members")));
    }

    @Test
    @DisplayName("Registration should generate unique registration numbers")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldGenerateUniqueRegistrationNumbers() throws Exception {
        // When: Registering both members
        MvcResult result1 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "2005-05-15",
                                            "nationality": "CZ",
                                            "gender": "MALE",
                                            "email": "jan1@example.com",
                                            "phone": "+420777111111",
                                            "address": {
                                                "street": "Street 1",
                                                "city": "City1",
                                                "postalCode": "10000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "firstName": "Petra",
                                            "lastName": "Nováková",
                                            "dateOfBirth": "2005-05-15",
                                            "nationality": "CZ",
                                            "gender": "FEMALE",
                                            "email": "petra1@example.com",
                                            "phone": "+420777222222",
                                            "address": {
                                                "street": "Street 2",
                                                "city": "City2",
                                                "postalCode": "20000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        // Then: Verify both have unique IDs
        String memberId1 = extractMemberId(result1);
        String memberId2 = extractMemberId(result2);

        assertThat(memberId1).isNotNull();
        assertThat(memberId2).isNotNull();
        assertThat(memberId1).isNotEqualTo(memberId2);
    }

    private String extractMemberId(MvcResult result) {
        String locationHeader = result.getResponse().getHeader(HttpHeaders.LOCATION);
        if (locationHeader != null) {
            return locationHeader.substring(locationHeader.lastIndexOf("/"));
        } else {
            throw new IllegalArgumentException("Missing location header");
        }
    }
}
