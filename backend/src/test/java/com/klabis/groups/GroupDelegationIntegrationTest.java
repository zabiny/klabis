package com.klabis.groups;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.security.JwtKeysConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.InvitationId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Group owners edit the profiles of members only through what their group delegates (integration)")
class GroupDelegationIntegrationTest {

    private static final String PHONE = "+420777123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private FreeGroupManagementPort groupPort;
    @Autowired
    private TrainingGroupManagementPort trainingGroupPort;
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
    private Person otherMember;
    private Person outsider;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void registerPeople() {
        owner = registerAdult("Vlastník");
        member = registerAdult("Člen");
        otherMember = registerAdult("Druhý");
        outsider = registerAdult("Cizí");
    }

    private Person registerAdult(String lastName) {
        LocalDate dateOfBirth = LocalDate.now().minusYears(30);
        String birthNumber = dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234";
        Member registered = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", lastName + unique(), dateOfBirth, "CZ", Gender.MALE),
                Address.of("Hlavní 1", "Brno", "60200", "CZ"),
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

    private FreeGroup groupWithMembers(Set<Authority> delegated, Person... members) {
        FreeGroup group = groupPort.createGroup("Přátelé", owner.id(), delegated);
        for (Person person : members) {
            groupPort.inviteMember(group.getId(), owner.id(), person.id());
            InvitationId invitation = groupPort.getGroup(group.getId()).getPendingInvitations().stream()
                    .filter(inv -> inv.isForMember(person.id()))
                    .findFirst().orElseThrow().getId();
            groupPort.acceptInvitation(group.getId(), invitation, person.id());
        }
        return group;
    }

    private ResultActions getMember(Person viewer, Person target) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", target.id().uuid())
                .header("Authorization", "Bearer " + viewer.token())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions patchPhone(Person editor, Person target) throws Exception {
        return mockMvc.perform(patch("/api/members/{id}", target.id().uuid())
                .header("Authorization", "Bearer " + editor.token())
                .contentType("application/json")
                .content("{\"phone\": \"" + PHONE + "\"}"));
    }

    @Test
    @DisplayName("owner of a group delegating profile editing opens a member's detail with the edit action and saves a change")
    void ownerEditsMember() throws Exception {
        groupWithMembers(Set.of(Authority.MEMBERS_EDIT_PROFILE), member);

        getMember(owner, member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthNumber").exists())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='birthNumber')].readOnly")
                        .value(true))
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());

        patchPhone(owner, member).andExpect(status().isNoContent());
        getMember(owner, member).andExpect(jsonPath("$.phone").value(PHONE));
    }

    @Test
    @DisplayName("owner cannot edit a member who does not belong to the group")
    void ownerCannotEditOutsider() throws Exception {
        groupWithMembers(Set.of(Authority.MEMBERS_EDIT_PROFILE), member);

        getMember(owner, outsider)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist())
                .andExpect(jsonPath("$.birthNumber").doesNotExist());
        patchPhone(owner, outsider).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a member of the group does not get the edit action on another member of the same group")
    void memberCannotEditOtherMember() throws Exception {
        groupWithMembers(Set.of(Authority.MEMBERS_EDIT_PROFILE), member, otherMember);

        getMember(member, otherMember)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
        patchPhone(member, otherMember).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("owner of a group that delegates nothing gets no edit action on its members")
    void ownerOfPlainGroupCannotEditMember() throws Exception {
        groupWithMembers(Set.of(), member);

        getMember(owner, member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist());
        patchPhone(owner, member).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("owner loses the permission as soon as the member leaves the group")
    void ownerLosesPermissionWhenMemberLeaves() throws Exception {
        FreeGroup group = groupWithMembers(Set.of(Authority.MEMBERS_EDIT_PROFILE), member);
        patchPhone(owner, member).andExpect(status().isNoContent());

        groupPort.removeMember(group.getId(), member.id(), member.id());

        patchPhone(owner, member).andExpect(status().isForbidden());
        getMember(owner, member).andExpect(jsonPath("$._templates.updateMember").doesNotExist());
    }

    @Test
    @DisplayName("a former owner loses the permission over the members")
    void formerOwnerLosesPermission() throws Exception {
        FreeGroup group = groupWithMembers(Set.of(Authority.MEMBERS_EDIT_PROFILE), member, otherMember);
        groupPort.addOwner(group.getId(), otherMember.id(), owner.id());
        patchPhone(otherMember, member).andExpect(status().isNoContent());

        groupPort.removeOwner(group.getId(), otherMember.id(), owner.id());

        patchPhone(otherMember, member).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("founder creates a group delegating profile editing through the API and the detail shows it")
    void founderCreatesDelegatingGroupThroughApi() throws Exception {
        String location = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType("application/json")
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("{\"name\": \"Přátelé\", \"delegatedAuthorities\": [\"MEMBERS:EDIT_PROFILE\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location)
                        .header("Authorization", "Bearer " + owner.token())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delegatedAuthorities[0]").value("MEMBERS:EDIT_PROFILE"));
    }

    @Test
    @DisplayName("invited member sees what the owners will hold over them before deciding")
    void invitationShowsDelegatedAuthorities() throws Exception {
        FreeGroup group = groupPort.createGroup("Přátelé", owner.id(), Set.of(Authority.MEMBERS_EDIT_PROFILE));
        groupPort.inviteMember(group.getId(), owner.id(), member.id());

        mockMvc.perform(get("/api/invitations/pending")
                        .header("Authorization", "Bearer " + member.token())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.pendingInvitationResponseList[0].delegatedAuthorities[0]")
                        .value("MEMBERS:EDIT_PROFILE"));
    }

    @Test
    @DisplayName("a trainer without MEMBERS:MANAGE gets no edit action on a trainee and cannot change their data")
    void trainerCannotEditTrainee() throws Exception {
        trainingGroupPort.createTrainingGroup(
                new TrainingGroup.CreateTrainingGroup("Dospělí", owner.id(), new AgeRange(18, 99)));
        // Creating the group enrols every member whose age fits, so no explicit addition is needed.
        assertThat(trainingGroupPort.findTrainingGroupOfMember(member.id())).isPresent();

        getMember(owner, member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist())
                .andExpect(jsonPath("$.birthNumber").doesNotExist());
        patchPhone(owner, member).andExpect(status().isForbidden());
    }
}
