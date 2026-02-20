package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.members.domain.Gender;
import com.klabis.members.infrastructure.restapi.AddressRequest;
import com.klabis.members.infrastructure.restapi.MemberManagementDtosTestDataBuilder;
import com.klabis.members.infrastructure.restapi.RegisterMemberRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

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

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";

    @Test
    @DisplayName("Registration of member should succeed")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCompleteRegistrationFlowForAdult() throws Exception {
        // Given: An adult member (18+ years old)
        RegisterMemberRequest request = MemberManagementDtosTestDataBuilder.registerMemberRequestWithDateOfBirth(
                LocalDate.of(1999, 1, 15));

        // When: Registering the member
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, Matchers.containsString("/api/members")));
    }

    @Test
    @DisplayName("Registration should generate unique registration numbers")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldGenerateUniqueRegistrationNumbers() throws Exception {
        // Given: Two members born in the same year
        LocalDate dateOfBirth = LocalDate.of(2005, 5, 15);

        AddressRequest address1 = new AddressRequest(
                "Street 1",
                "City1",
                "10000",
                "CZ"
        );

        AddressRequest address2 = new AddressRequest(
                "Street 2",
                "City2",
                "20000",
                "CZ"
        );

        RegisterMemberRequest request1 = new RegisterMemberRequest(
                "Jan",
                "Novák",
                dateOfBirth,
                "CZ",
                Gender.MALE,
                "jan1@example.com",
                "+420777111111",
                address1,
                null,
                null,
                null);

        RegisterMemberRequest request2 = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                dateOfBirth,
                "CZ",
                Gender.FEMALE,
                "petra1@example.com",
                "+420777222222",
                address2,
                null,
                null,
                null);

        // When: Registering both members
        MvcResult result1 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request1))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request2))
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
