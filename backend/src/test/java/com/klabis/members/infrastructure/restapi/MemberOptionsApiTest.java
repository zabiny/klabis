package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.domain.Member;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/members/options")
@MembersWebMvcTest
class MemberOptionsApiTest extends MemberApiTestBase {

    @Test
    @DisplayName("should return 200 with list of active members as value+prompt pairs")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldReturnActiveMembers() throws Exception {
        UUID memberId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID memberId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Member activeMember1 = MemberTestDataBuilder.aMemberWithId(memberId1)
                .withName("Jan", "Novák")
                .withRegistrationNumber("ZBM0001")
                .withActive(true)
                .build();
        Member activeMember2 = MemberTestDataBuilder.aMemberWithId(memberId2)
                .withName("Eva", "Svobodová")
                .withRegistrationNumber("ZBM0002")
                .withActive(true)
                .build();

        when(managementService.listActiveMembers()).thenReturn(List.of(activeMember1, activeMember2));

        mockMvc.perform(get("/api/members/options")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].value").value(memberId1.toString()))
                .andExpect(jsonPath("$[0].prompt").value("Jan Novák (ZBM0001)"))
                .andExpect(jsonPath("$[1].value").value(memberId2.toString()))
                .andExpect(jsonPath("$[1].prompt").value("Eva Svobodová (ZBM0002)"));
    }
}
