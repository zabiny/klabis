package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("X-Warnings header for birth number consistency")
@MembersWebMvcTest
class MemberBirthNumberWarningsApiTest extends MemberApiTestBase {

    @Autowired
    private RegistrationPort registrationService;

    private final String REGISTER_BODY = """
            {
                "firstName": "Jana",
                "lastName": "Nováková",
                "dateOfBirth": "1990-05-15",
                "nationality": "CZ",
                "gender": "FEMALE",
                "email": "jana.novakova@example.com",
                "phone": "+420777123456",
                "address": {
                    "street": "Hlavní 123",
                    "city": "Praha",
                    "postalCode": "11000",
                    "country": "CZ"
                },
                "birthNumber": "905101/1239"
            }
            """;

    @Test
    @DisplayName("POST /api/members should include X-Warnings header when birth number date mismatches dateOfBirth")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldIncludeWarningsHeaderWhenBirthNumberDateMismatches() throws Exception {
        UUID memberId = UUID.randomUUID();
        // Member with dateOfBirth 1990-05-15 but birth number 905101 implies 1990-01-01 → date mismatch
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withGender(Gender.FEMALE)
                .withNationality("CZ")
                .withBirthNumber("905101/1239")
                .build();
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Warnings"));
    }

    @Test
    @DisplayName("POST /api/members should not include X-Warnings header when birth number is consistent")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldNotIncludeWarningsHeaderWhenBirthNumberIsConsistent() throws Exception {
        UUID memberId = UUID.randomUUID();
        // Member with dateOfBirth 1990-01-01 and birth number 905101 (female, 1990-01-01) → consistent
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withGender(Gender.FEMALE)
                .withNationality("CZ")
                .withBirthNumber("905101/1239")
                .build();
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("X-Warnings"));
    }

    @Test
    @DisplayName("PATCH /api/members/{id} should include X-Warnings header when birth number gender mismatches")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldIncludeWarningsHeaderOnUpdateWhenGenderMismatches() throws Exception {
        UUID memberId = UUID.randomUUID();
        // Member is MALE but birth number 905101 indicates FEMALE → gender mismatch warning
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withGender(Gender.MALE)
                .withNationality("CZ")
                .withBirthNumber("905101/1239")
                .build();
        when(managementService.prefilledUpdateCommand(any(MemberId.class)))
                .thenReturn(Member.UpdateMember.from(member));
        when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class))).thenReturn(member);

        mockMvc.perform(patch("/api/members/{id}", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "birthNumber": "905101/1239"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Warnings"));
    }

    @Test
    @DisplayName("PATCH /api/members/{id} should not include X-Warnings when no birth number provided")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldNotIncludeWarningsWhenNoBirthNumberOnUpdate() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .build();
        when(managementService.prefilledUpdateCommand(any(MemberId.class)))
                .thenReturn(Member.UpdateMember.from(member));
        when(managementService.updateMember(any(MemberId.class), any(Member.UpdateMember.class))).thenReturn(member);

        mockMvc.perform(patch("/api/members/{id}", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("X-Warnings"));
    }
}
