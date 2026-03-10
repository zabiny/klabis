package com.klabis.members.infrastructure.restapi;

import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.BirthNumberAuditPublisher;
import com.klabis.members.application.ManagementService;
import com.klabis.members.application.RegistrationService;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GDPR birth number audit event publishing from REST controllers.
 */
@DisplayName("Birth Number Audit – Controller Tests")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class})
@Import({MemberMapperImpl.class})
@MockitoBean(types = {UserService.class, UserDetailsService.class})
class BirthNumberAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private BirthNumberAuditPublisher birthNumberAuditPublisher;

    @TestBean
    private EntityLinks entityLinks;

    static EntityLinks entityLinks() {
        return HateoasTestingSupport.createModuleEntityLinks(MemberController.class);
    }

    @Nested
    @DisplayName("GET /api/members/{id} – VIEW audit")
    class GetMemberAuditTests {

        @Test
        @DisplayName("should publish VIEW_BIRTH_NUMBER event when member has birth number")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldPublishViewEventWhenMemberHasBirthNumber() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withBirthNumber(BirthNumber.of("900101/1234"))
                    .withNoGuardian()
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(get("/api/members/{id}", memberId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            verify(birthNumberAuditPublisher).publishViewed(any(), eq(new MemberId(memberId)));
        }

        @Test
        @DisplayName("should NOT publish audit event when member has no birth number")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldNotPublishEventWhenMemberHasNoBirthNumber() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(get("/api/members/{id}", memberId).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());

            verifyNoInteractions(birthNumberAuditPublisher);
        }
    }

    @Nested
    @DisplayName("PATCH /api/members/{id} – MODIFY audit")
    class PatchMemberAuditTests {

        @Test
        @DisplayName("should publish MODIFY_BIRTH_NUMBER event when birthNumber field is provided")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_UPDATE})
        void shouldPublishModifyEventWhenBirthNumberIsProvided() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member existingMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMemberByAdmin.class)))
                    .thenReturn(existingMember);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"birthNumber\": \"900101/1234\"}"))
                    .andExpect(status().isNoContent());

            verify(birthNumberAuditPublisher).publishModified(any(), eq(new MemberId(memberId)));
        }

        @Test
        @DisplayName("should NOT publish audit event when birthNumber field is not provided")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_UPDATE})
        void shouldNotPublishEventWhenBirthNumberNotInRequest() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member existingMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withNationality("CZ")
                    .withNoGuardian()
                    .build();

            when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMemberByAdmin.class)))
                    .thenReturn(existingMember);

            mockMvc.perform(patch("/api/members/{id}", memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"new@example.com\"}"))
                    .andExpect(status().isNoContent());

            verifyNoInteractions(birthNumberAuditPublisher);
        }
    }

    @Nested
    @DisplayName("POST /api/members – MODIFY audit on registration")
    class RegisterMemberAuditTests {

        @Test
        @DisplayName("should publish MODIFY_BIRTH_NUMBER event when birthNumber is provided during registration")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_CREATE})
        void shouldPublishModifyEventWhenBirthNumberProvidedOnRegistration() throws Exception {
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

            verify(birthNumberAuditPublisher).publishModified(any(), eq(new MemberId(newMemberId)));
        }

        @Test
        @DisplayName("should NOT publish audit event when birthNumber is not provided during registration")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_CREATE})
        void shouldNotPublishEventWhenNoBirthNumberOnRegistration() throws Exception {
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

            verifyNoInteractions(birthNumberAuditPublisher);
        }
    }
}
