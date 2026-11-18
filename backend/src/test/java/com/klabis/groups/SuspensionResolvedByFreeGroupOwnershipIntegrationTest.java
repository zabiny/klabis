package com.klabis.groups;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.security.JwtKeysConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Suspension blocked by sole free group owner is resolved by a manager (integration)")
class SuspensionResolvedByFreeGroupOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private FreeGroupManagementPort groupPort;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtEncoder jwtEncoder;

    @Value(JwtKeysConfiguration.ISSUER_PROPERTY)
    private String issuer;

    private record Person(Member member, String token) {
        MemberId id() {
            return member.getId();
        }
    }

    private Person owner;
    private Person successor;
    private FreeGroup group;
    private String groupPath;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void setUp() {
        owner = registerAdult("Vlastník");
        successor = registerAdult("Nástupce");
        group = groupPort.createGroup("Přátelé", owner.id(), Set.of());
        groupPort.inviteMember(group.getId(), owner.id(), successor.id());
        var invitation = groupPort.getGroup(group.getId()).getPendingInvitations().stream()
                .filter(inv -> inv.isForMember(successor.id())).findFirst().orElseThrow().getId();
        groupPort.acceptInvitation(group.getId(), invitation, successor.id());
        groupPath = "/api/groups/" + group.getId().uuid();
    }

    private Person registerAdult(String lastName) {
        LocalDate dateOfBirth = LocalDate.now().minusYears(30);
        String birthNumber = dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234";
        Member registered = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", lastName + unique(), dateOfBirth, Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Brno", "60200", Country.of("CZ")),
                EmailAddress.of("jan." + unique() + "@example.com"), PhoneNumber.of("+420777000111"),
                BirthNumber.of(birthNumber), null, null));
        UserId userId = registered.getId().toUserId();
        User user = userRepository.findById(userId).orElseThrow();
        userRepository.save(user.activateWithPassword("{noop}unused"));
        String username = userRepository.findById(userId).orElseThrow().getUsername();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .claim("user_id", userId.uuid().toString())
                .claim("member_id", registered.getId().uuid().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        return new Person(registered, token);
    }

    private ResultActions suspendOwner() throws Exception {
        return mockMvc.perform(post("/api/members/{id}/suspend", owner.id().uuid())
                .contentType("application/json")
                .content("{\"reason\": \"ODHLASKA\"}"));
    }

    private void expectBlockedByGroup() throws Exception {
        suspendOwner()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupType").value("FREE"))
                .andExpect(jsonPath("$.groups.affectedGroups[0].groupLink").value("http://localhost" + groupPath));
    }

    @Test
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("manager adds a member as owner, suspension succeeds, promoted owner then renames the group")
    void suspensionSucceedsAfterManagerAddsOwner() throws Exception {
        expectBlockedByGroup();
        rename(successor).andExpect(status().isForbidden());

        mockMvc.perform(get(groupPath).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.addGroupOwner").exists())
                .andExpect(jsonPath("$._templates.deleteGroup").exists());

        mockMvc.perform(post(groupPath + "/owners")
                        .contentType("application/json")
                        .content("{\"memberId\": \"%s\"}".formatted(successor.id().uuid())))
                .andExpect(status().isNoContent());

        suspendOwner().andExpect(status().isNoContent());

        rename(successor).andExpect(status().isNoContent());
    }

    private ResultActions rename(Person caller) throws Exception {
        return mockMvc.perform(patch(groupPath)
                .with(request -> {
                    request.removeHeader("Authorization");
                    request.addHeader("Authorization", "Bearer " + caller.token());
                    return request;
                })
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .contentType("application/json")
                .content("{\"name\": \"Nový název\"}"));
    }

    @Test
    @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("manager dissolves the group, suspension succeeds")
    void suspensionSucceedsAfterManagerDissolvesGroup() throws Exception {
        expectBlockedByGroup();

        mockMvc.perform(delete(groupPath)).andExpect(status().isNoContent());

        suspendOwner().andExpect(status().isNoContent());
    }
}
