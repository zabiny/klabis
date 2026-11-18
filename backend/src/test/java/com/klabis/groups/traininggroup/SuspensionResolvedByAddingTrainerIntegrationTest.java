package com.klabis.groups.traininggroup;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Suspension blocked by last trainer is resolved by adding a trainer (integration)")
class SuspensionResolvedByAddingTrainerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private TrainingGroupManagementPort trainingGroupPort;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Member registerAdult(String lastName) {
        LocalDate dateOfBirth = LocalDate.now().minusYears(30);
        String birthNumber = dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234";
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", lastName + unique(), dateOfBirth, Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Brno", "60200", Country.of("CZ")),
                EmailAddress.of("jan." + unique() + "@example.com"), PhoneNumber.of("+420777000111"),
                BirthNumber.of(birthNumber), null, null));
    }

    private ResultActions suspend(MemberId memberId) throws Exception {
        return mockMvc.perform(post("/api/members/{id}/suspend", memberId.uuid())
                .contentType("application/json")
                .content("{\"reason\": \"ODHLASKA\"}"));
    }

    @Test
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE, Authority.GROUPS_TRAINING})
    @DisplayName("409 lists the group with a link to it, addTrainer is offered there, and suspension then succeeds")
    void suspensionSucceedsAfterSuccessorTrainerIsAdded() throws Exception {
        Member trainer = registerAdult("Trenér");
        Member successor = registerAdult("Nástupce");
        TrainingGroup group = trainingGroupPort.createTrainingGroup(
                new TrainingGroup.CreateTrainingGroup("Žáci", trainer.getId(), new AgeRange(10, 12)));
        String groupPath = "/api/training-groups/" + group.getId().uuid();

        suspend(trainer.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupType").value("TRAINING"))
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupLink").value("http://localhost" + groupPath));

        mockMvc.perform(get(groupPath).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.addTrainer").exists());

        mockMvc.perform(post(groupPath + "/trainers")
                        .contentType("application/json")
                        .content("{\"memberId\": \"%s\"}".formatted(successor.getId().uuid())))
                .andExpect(status().isNoContent());

        suspend(trainer.getId()).andExpect(status().isNoContent());
    }
}
