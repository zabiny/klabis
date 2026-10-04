package com.klabis.members;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.security.JwtKeysConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("A non-member legal guardian opens and edits their child's profile (integration)")
class NonMemberGuardianEditsChildIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value(JwtKeysConfiguration.ISSUER_PROPERTY)
    private String issuer;

    private Member child;
    private String guardianToken;
    private UserId guardianId;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void registerChildWithNonMemberGuardian() {
        LocalDate dateOfBirth = LocalDate.now().minusYears(10);
        child = registerMinor("Dítě" + unique(), dateOfBirth, List.of(GuardianInput.created(
                new NewLegalGuardian("Eva", "Svobodová", "eva." + unique() + "@example.com", "+420777111222"))));
        guardianId = legalGuardianGroupPort.guardiansOf(child.getId()).iterator().next();
        guardianToken = tokenOfActivatedGuardian(guardianId);
    }

    private Member registerMinor(String lastName, LocalDate dateOfBirth, List<GuardianInput> guardians) {
        String birthNumber = dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234";
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", lastName, dateOfBirth, "CZ", Gender.MALE),
                Address.of("Dětská 1", "Brno", "60200", "CZ"),
                null, null, BirthNumber.of(birthNumber), null, null, guardians, null));
    }

    private String tokenOfActivatedGuardian(UserId userId) {
        User user = userRepository.findById(userId).orElseThrow();
        userRepository.save(user.activateWithPassword("{noop}unused"));
        String username = userRepository.findById(userId).orElseThrow().getUsername();
        assertThat(username).startsWith("EXT");
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .claim("user_id", userId.uuid().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    private ResultActions getMember(UUID id, String token) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", id)
                .header("Authorization", "Bearer " + token)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions patchPhone(UUID id, String token, String phone) throws Exception {
        return mockMvc.perform(patch("/api/members/{id}", id)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"phone\": \"" + phone + "\"}"));
    }

    @Test
    @DisplayName("guardian opens the child's detail with all data and the edit action")
    void guardianOpensChildDetail() throws Exception {
        getMember(child.getId().uuid(), guardianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthNumber").exists())
                .andExpect(jsonPath("$.dateOfBirth").exists())
                .andExpect(jsonPath("$.gender").exists())
                .andExpect(jsonPath("$._links.legalGuardians.href").exists())
                .andExpect(jsonPath("$._templates.updateMember.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='birthNumber')].readOnly")
                        .value(true))
                .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
    }

    @Test
    @DisplayName("guardian saves a change of the child's telephone and it shows on the detail")
    void guardianEditsChildPhone() throws Exception {
        patchPhone(child.getId().uuid(), guardianToken, "+420777123456").andExpect(status().isNoContent());

        getMember(child.getId().uuid(), guardianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+420777123456"));
        assertThat(memberRepository.findById(child.getId()).orElseThrow().getPhone().value())
                .isEqualTo("+420777123456");
    }

    @Test
    @DisplayName("guardian cannot change the child's reserved data")
    void guardianCannotChangeReservedData() throws Exception {
        mockMvc.perform(patch("/api/members/{id}", child.getId().uuid())
                        .header("Authorization", "Bearer " + guardianToken)
                        .contentType("application/json")
                        .content("{\"lastName\": \"Changed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("guardian's view of the child's birth number is audited")
    void guardianViewOfBirthNumberIsAudited() throws Exception {
        getMember(child.getId().uuid(), guardianToken).andExpect(status().isOk());

        await().untilAsserted(() -> assertThat(jdbcTemplate.queryForList(
                "SELECT action FROM members.birth_number_audit_log WHERE user_id = ? AND member_id = ?",
                guardianId.uuid(), child.getId().uuid())).extracting(row -> row.get("action"))
                .contains("VIEW_BIRTH_NUMBER"));
    }

    @Test
    @DisplayName("guardian of one child cannot edit an unrelated minor and sees no edit action there")
    void guardianCannotEditUnrelatedMinor() throws Exception {
        Member otherChild = registerMinor("Cizí" + unique(), LocalDate.now().minusYears(11), List.of(GuardianInput.created(
                new NewLegalGuardian("Karel", "Cizí", "karel." + unique() + "@example.com", "+420777333444"))));

        patchPhone(otherChild.getId().uuid(), guardianToken, "+420777123456").andExpect(status().isForbidden());
        getMember(otherChild.getId().uuid(), guardianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember").doesNotExist())
                .andExpect(jsonPath("$.birthNumber").doesNotExist());
    }

    @Test
    @DisplayName("guardian loses the permission when removed from the child's guardians")
    void removedGuardianLosesPermission() throws Exception {
        Member sibling = registerMinor("Sourozenec" + unique(), LocalDate.now().minusYears(12), List.of(GuardianInput.created(
                new NewLegalGuardian("Karel", "Dvořák", "karel." + unique() + "@example.com", "+420777333444"))));
        UserId otherGuardian = legalGuardianGroupPort.guardiansOf(sibling.getId()).iterator().next();

        legalGuardianGroupPort.setGuardiansOf(child.getId(), List.of(GuardianInput.existing(otherGuardian)));

        patchPhone(child.getId().uuid(), guardianToken, "+420777123456").andExpect(status().isForbidden());
    }
}
