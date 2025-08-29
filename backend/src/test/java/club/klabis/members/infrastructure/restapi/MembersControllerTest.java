package club.klabis.members.infrastructure.restapi;

import club.klabis.adapters.api.ApiTestConfiguration;
import club.klabis.members.application.EditMemberInfoUseCase;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.application.MembershipSuspendUseCase;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.members.domain.forms.RegistrationForm;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// TODO: fix once settled up modulith structure
@Disabled
@WebMvcTest(controllers = MembersApi.class)
@Import(ApiTestConfiguration.class)
class MembersControllerTest {

    @MockitoBean
    private MembersRepository membersRepositoryMock;
    @MockitoBean
    private MembershipSuspendUseCase membershipSuspendUseCaseMock;
    @MockitoBean
    private EditMemberInfoUseCase editMemberUseCaseMock;
    @MockitoBean
    private UserGrantsUpdateUseCase userGrantsUpdateUseCaseMock;
    @MockitoBean
    private ApplicationUsersRepository applicationUsersRepositoryMock;


    @Autowired
    private MockMvc mockMvc;

    @DisplayName("GET /members tests")
    @Nested
    class GetMembersTests {

        private static Member createMember() {
            RegistrationForm registrationForm = RegistrationFormBuilder.builder().firstName("Test").lastName("Something").registrationNumber(
                    RegistrationNumber.ofRegistrationId("ZBM8000")).dateOfBirth(LocalDate.of(1970, 10, 21)).build();
            return Member.fromRegistration(registrationForm);
        }

        @DisplayName("it should return full view data")
        @Test
        void itShouldReturnFullViewData() throws Exception {

            Member data = createMember();
            ReflectionTestUtils.setField(data, "firstName", "Test");
            ReflectionTestUtils.setField(data, "lastName", "User");
            ReflectionTestUtils.setField(data, "registration", RegistrationNumber.ofRegistrationId("ZBM8000"));
            ReflectionTestUtils.setField(data, "dateOfBirth", LocalDate.of(1970, 10, 21));

            when(membersRepositoryMock.findAll(false)).thenReturn(List.of(data));

            mockMvc.perform(get("/members?view=full"))
                    .andExpect(jsonPath("$.firstName").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("Something"))
                    .andExpect(jsonPath("$.registrationNumber").value("ZBM8000"))
                    .andExpect(jsonPath("$.dateOfBirth").value("1970-10-21"));
        }

        @DisplayName("it should return compact view data")
        @Test
        void itShouldReturnCompactViewData() throws Exception {

            Member data = createMember();
            ReflectionTestUtils.setField(data, "firstName", "Test");
            ReflectionTestUtils.setField(data, "lastName", "User");
            ReflectionTestUtils.setField(data, "registration", RegistrationNumber.ofRegistrationId("ZBM8000"));
            ReflectionTestUtils.setField(data, "dateOfBirth", LocalDate.of(1970, 10, 21));

            when(membersRepositoryMock.findAll(false)).thenReturn(List.of(data));

            mockMvc.perform(get("/members?view=compact"))
                    .andExpect(jsonPath("$.firstName").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("Something"))
                    .andExpect(jsonPath("$.registrationNumber").value("ZBM8000"))
                    .andExpect(jsonPath("$.dateOfBirth").doesNotExist());
        }


    }

}