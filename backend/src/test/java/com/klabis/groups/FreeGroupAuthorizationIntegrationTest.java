package com.klabis.groups;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.security.JwtKeysConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.InvitationId;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Free group operations by role: owner, member, outsider (integration regression net)")
class FreeGroupAuthorizationIntegrationTest {

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
    private Person member;
    private Person outsider;
    private Person candidate;
    private FreeGroup group;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void setUp() {
        owner = registerAdult("Vlastník");
        member = registerAdult("Člen");
        outsider = registerAdult("Cizí");
        candidate = registerAdult("Kandidát");
        group = groupPort.createGroup("Přátelé", owner.id(), Set.of());
        invite(owner, member);
        accept(member);
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

    private void invite(Person by, Person target) {
        groupPort.inviteMember(group.getId(), by.id(), target.id());
    }

    private void accept(Person target) {
        InvitationId invitation = pendingInvitationFor(target);
        groupPort.acceptInvitation(group.getId(), invitation, target.id());
    }

    private InvitationId pendingInvitationFor(Person target) {
        return groupPort.getGroup(group.getId()).getPendingInvitations().stream()
                .filter(inv -> inv.isForMember(target.id()))
                .findFirst().orElseThrow().getId();
    }

    private ResultActions call(Person caller, HttpMethod method, String path, String body) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, path, group.getId().uuid())
                .header("Authorization", "Bearer " + caller.token())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        if (body != null) {
            request.contentType("application/json").content(body);
        }
        return mockMvc.perform(request);
    }

    private String renameBody() {
        return "{\"name\": \"Nový název\"}";
    }

    private String memberBody(Person person) {
        return "{\"memberId\": \"" + person.id().uuid() + "\"}";
    }

    @Nested
    @DisplayName("PATCH /api/groups/{id}")
    class Rename {

        @Test
        @DisplayName("owner renames, member and outsider are rejected")
        void ownerOnly() throws Exception {
            call(member, HttpMethod.PATCH, "/api/groups/{id}", renameBody()).andExpect(status().isForbidden());
            call(outsider, HttpMethod.PATCH, "/api/groups/{id}", renameBody()).andExpect(status().isForbidden());
            call(owner, HttpMethod.PATCH, "/api/groups/{id}", renameBody()).andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}")
    class Delete {

        @Test
        @DisplayName("owner deletes, member and outsider are rejected")
        void ownerOnly() throws Exception {
            call(member, HttpMethod.DELETE, "/api/groups/{id}", null).andExpect(status().isForbidden());
            call(outsider, HttpMethod.DELETE, "/api/groups/{id}", null).andExpect(status().isForbidden());
            call(owner, HttpMethod.DELETE, "/api/groups/{id}", null).andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/owners")
    class AddOwner {

        @Test
        @DisplayName("owner promotes a member, member and outsider are rejected")
        void ownerOnly() throws Exception {
            call(member, HttpMethod.POST, "/api/groups/{id}/owners", memberBody(member))
                    .andExpect(status().isForbidden());
            call(outsider, HttpMethod.POST, "/api/groups/{id}/owners", memberBody(member))
                    .andExpect(status().isForbidden());
            call(owner, HttpMethod.POST, "/api/groups/{id}/owners", memberBody(member))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/owners/{memberId}")
    class RemoveOwner {

        @Test
        @DisplayName("owner removes a co-owner, member and outsider are rejected")
        void ownerOnly() throws Exception {
            groupPort.addOwner(group.getId(), member.id());
            String path = "/api/groups/{id}/owners/" + member.id().uuid();

            call(outsider, HttpMethod.DELETE, path, null).andExpect(status().isForbidden());
            call(owner, HttpMethod.DELETE, path, null).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("a plain member cannot remove an owner")
        void memberRejected() throws Exception {
            String path = "/api/groups/{id}/owners/" + owner.id().uuid();

            call(member, HttpMethod.DELETE, path, null).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/groups/{id}/invitations")
    class Invite {

        @Test
        @DisplayName("owner invites, outsider is rejected")
        void ownerAllowedOutsiderRejected() throws Exception {
            call(outsider, HttpMethod.POST, "/api/groups/{id}/invitations", memberBody(candidate))
                    .andExpect(status().isForbidden());
            call(owner, HttpMethod.POST, "/api/groups/{id}/invitations", memberBody(candidate))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("a non-owner member cannot invite")
        void memberRejected() throws Exception {
            call(member, HttpMethod.POST, "/api/groups/{id}/invitations", memberBody(candidate))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/invitations/{invitationId}")
    class CancelInvitation {

        @Test
        @DisplayName("owner cancels, member and outsider are rejected")
        void ownerOnly() throws Exception {
            invite(owner, candidate);
            String path = "/api/groups/{id}/invitations/" + pendingInvitationFor(candidate).value();

            call(member, HttpMethod.DELETE, path, null).andExpect(status().isForbidden());
            call(outsider, HttpMethod.DELETE, path, null).andExpect(status().isForbidden());
            call(owner, HttpMethod.DELETE, path, null).andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/groups/{id}/members/{memberId}")
    class RemoveMember {

        @Test
        @DisplayName("owner removes another member, member removes themselves, member cannot remove others, outsider is rejected")
        void ownerOrSelf() throws Exception {
            Person second = registerAdult("Druhý");
            invite(owner, second);
            accept(second);
            String removeSecond = "/api/groups/{id}/members/" + second.id().uuid();
            String removeMember = "/api/groups/{id}/members/" + member.id().uuid();

            call(member, HttpMethod.DELETE, removeSecond, null).andExpect(status().isForbidden());
            call(outsider, HttpMethod.DELETE, removeSecond, null).andExpect(status().isForbidden());
            call(owner, HttpMethod.DELETE, removeSecond, null).andExpect(status().isNoContent());
            call(member, HttpMethod.DELETE, removeMember, null).andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/groups/{id}")
    class Get {

        @Test
        @DisplayName("owner and member read the group, outsider is rejected")
        void ownerAndMemberOnly() throws Exception {
            call(outsider, HttpMethod.GET, "/api/groups/{id}", null).andExpect(status().isForbidden());
            call(member, HttpMethod.GET, "/api/groups/{id}", null).andExpect(status().isOk());
            call(owner, HttpMethod.GET, "/api/groups/{id}", null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("owner's self link carries all owner templates and no leave-group")
        void ownerTemplates() throws Exception {
            call(owner, HttpMethod.GET, "/api/groups/{id}", null)
                    .andExpect(jsonPath("$._templates.updateGroup").exists())
                    .andExpect(jsonPath("$._templates.deleteGroup").exists())
                    .andExpect(jsonPath("$._templates.addGroupOwner").exists())
                    .andExpect(jsonPath("$._templates.inviteMember").exists())
                    .andExpect(jsonPath("$._templates.removeGroupMember").doesNotExist());
        }

        @Test
        @DisplayName("member's self link carries only the leave-group template")
        void memberTemplates() throws Exception {
            call(member, HttpMethod.GET, "/api/groups/{id}", null)
                    .andExpect(jsonPath("$._templates.removeGroupMember").exists())
                    .andExpect(jsonPath("$._templates.updateGroup").doesNotExist())
                    .andExpect(jsonPath("$._templates.deleteGroup").doesNotExist())
                    .andExpect(jsonPath("$._templates.addGroupOwner").doesNotExist())
                    .andExpect(jsonPath("$._templates.inviteMember").doesNotExist());
        }

        @Test
        @DisplayName("pending invitations are listed with a cancel template to the owner only")
        void pendingInvitations() throws Exception {
            invite(owner, candidate);

            call(owner, HttpMethod.GET, "/api/groups/{id}", null)
                    .andExpect(jsonPath("$.pendingInvitations[0]._templates.cancelInvitation").exists());
            call(member, HttpMethod.GET, "/api/groups/{id}", null)
                    .andExpect(jsonPath("$.pendingInvitations").isEmpty());
        }
    }
}
