package com.klabis.members;

import com.klabis.CrossModuleTest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CrossModuleTest(extraIncludes = "groups")
@DisplayName("Importing a member brings its usual consequences")
class ImportMemberUsualConsequencesTest {

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("assigns the member to the age-matching training group")
    @WithKlabisMockUser(authorities = {Authority.GROUPS_TRAINING})
    void importedMemberIsAssignedToAgeMatchingTrainingGroup() throws Exception {
        String groupLocation = mockMvc.perform(post("/api/training-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Import Consequences Group", "trainerId": "%s", "minAge": 6, "maxAge": 15}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        LocalDate dateOfBirth = LocalDate.now().minus(Period.ofYears(12));
        RegistrationPort.RegisterNewMember details = new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Imported", "Child", dateOfBirth, Nationality.of("CZ"), Gender.MALE),
                Address.of("Importovaná 1", "Praha", "10000", Country.of("CZ")),
                EmailAddress.of("imported.consequences.test@example.com"),
                PhoneNumber.of("+420777123456"),
                BirthNumber.of(dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234"),
                null,
                null
        );
        Member member = registrationPort.importMember(
                new RegistrationPort.ImportMember(details, RegistrationNumber.of("ZBM0098")));

        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInSameThread().untilAsserted(() -> {
            String groupJson = mockMvc.perform(get(groupLocation).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(JsonPath.<List<String>>read(groupJson, "$.members[*].memberId"))
                    .contains(member.getId().uuid().toString());
        });
    }
}
