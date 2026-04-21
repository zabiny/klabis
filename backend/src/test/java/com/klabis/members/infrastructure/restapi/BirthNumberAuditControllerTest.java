package com.klabis.members.infrastructure.restapi;

import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GDPR birth number audit event publishing from REST controllers.
 * Verifies that audit-triggering calls are delegated correctly to services
 * so they are captured by Spring Modulith outbox within a transaction.
 */
@DisplayName("Birth Number Audit – Controller Tests")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class})
@Import({MemberMapperImpl.class})
@WithPostprocessors
class BirthNumberAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementPort managementService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RegistrationPort registrationService;

    @TestBean
    private EntityLinks entityLinks;

    static EntityLinks entityLinks() {
        return HateoasTestingSupport.createModuleEntityLinks(MemberController.class);
    }

    @Nested
    @DisplayName("GET /api/members/{id} – VIEW audit")
    class GetMemberAuditTests {

        @Test
        @DisplayName("should delegate to getMemberAndRecordView so VIEW audit runs in transaction")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldCallGetMemberAndRecordViewForViewAudit() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withBirthNumber(BirthNumber.of("900101/1234"))
                    .withNoGuardian()
                    .build();

            when(managementService.getMemberAndRecordView(any(MemberId.class), any(UserId.class), anyBoolean()))
                    .thenReturn(member);

            mockMvc.perform(get("/api/members/{id}", memberId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            verify(managementService).getMemberAndRecordView(eq(new MemberId(memberId)), any(UserId.class), anyBoolean());
        }
    }

    @Nested
    @DisplayName("PATCH /api/members/{id} – MODIFY audit")
    class PatchMemberAuditTests {

        @Test
        @DisplayName("should pass non-null updatedBy in admin command when birthNumber is in request")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassUpdatedByWhenBirthNumberIsProvided() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member existingMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class)))
                    .thenReturn(existingMember);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"birthNumber\": \"900101/1234\"}"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Member.UpdateMember> commandCaptor =
                    ArgumentCaptor.forClass(Member.UpdateMember.class);
            verify(managementService).updateMember(eq(new MemberId(memberId)), commandCaptor.capture());

            Member.UpdateMember command = commandCaptor.getValue();
            assertThat(command.birthNumber()).isNotNull();
            assertThat(command.updatedBy()).isNotNull();
        }

        @Test
        @DisplayName("should pass non-null updatedBy in admin command even when birthNumber is absent")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassUpdatedByEvenWhenBirthNumberNotInRequest() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member existingMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class)))
                    .thenReturn(existingMember);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"new@example.com\"}"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<Member.UpdateMember> commandCaptor =
                    ArgumentCaptor.forClass(Member.UpdateMember.class);
            verify(managementService).updateMember(eq(new MemberId(memberId)), commandCaptor.capture());

            Member.UpdateMember command = commandCaptor.getValue();
            assertThat(command.birthNumber()).isNull();
            assertThat(command.updatedBy()).isNotNull();
        }
    }

    @Nested
    @DisplayName("POST /api/members – MODIFY audit on registration")
    class RegisterMemberAuditTests {

        @Test
        @DisplayName("should pass non-null registeredBy in registration command when birthNumber is provided")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassRegisteredByWhenBirthNumberProvidedOnRegistration() throws Exception {
            UUID newMemberId = UUID.randomUUID();
            Member registeredMember = MemberTestDataBuilder.aMemberWithId(newMemberId)
                    .withNationality("CZ")
                    .withBirthNumber(BirthNumber.of("900101/1234"))
                    .withNoGuardian()
                    .build();

            when(registrationService.registerMember(any())).thenReturn(registeredMember);

            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jan",
                                        "lastName": "Novák",
                                        "dateOfBirth": "1990-01-01",
                                        "gender": "MALE",
                                        "nationality": "CZ",
                                        "email": "jan.novak@example.com",
                                        "phone": "+420123456789",
                                        "address": {"street": "Hlavní 1", "city": "Praha", "postalCode": "11000", "country": "CZ"},
                                        "birthNumber": "900101/1234"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            ArgumentCaptor<RegistrationPort.RegisterNewMember> commandCaptor =
                    ArgumentCaptor.forClass(RegistrationPort.RegisterNewMember.class);
            verify(registrationService).registerMember(commandCaptor.capture());

            RegistrationPort.RegisterNewMember command = commandCaptor.getValue();
            assertThat(command.birthNumber()).isNotNull();
            assertThat(command.registeredBy()).isNotNull();
        }

        @Test
        @DisplayName("should pass non-null registeredBy in registration command even without birthNumber")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_MANAGE})
        void shouldPassRegisteredByEvenWhenNoBirthNumberOnRegistration() throws Exception {
            UUID newMemberId = UUID.randomUUID();
            Member registeredMember = MemberTestDataBuilder.aMemberWithId(newMemberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(registrationService.registerMember(any())).thenReturn(registeredMember);

            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Jan",
                                        "lastName": "Novák",
                                        "dateOfBirth": "1990-01-01",
                                        "gender": "MALE",
                                        "nationality": "CZ",
                                        "email": "jan.novak@example.com",
                                        "phone": "+420123456789",
                                        "address": {"street": "Hlavní 1", "city": "Praha", "postalCode": "11000", "country": "CZ"}
                                    }
                                    """))
                    .andExpect(status().isCreated());

            ArgumentCaptor<RegistrationPort.RegisterNewMember> commandCaptor =
                    ArgumentCaptor.forClass(RegistrationPort.RegisterNewMember.class);
            verify(registrationService).registerMember(commandCaptor.capture());

            RegistrationPort.RegisterNewMember command = commandCaptor.getValue();
            assertThat(command.birthNumber()).isNull();
            assertThat(command.registeredBy()).isNotNull();
        }
    }
}
