package com.klabis.common.authorization;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.security.JwtKeysConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Permission changes take effect on the next request of an already issued access token")
class PermissionChangesTakeEffectIntegrationTest {

    private static final String TRAINING_GROUPS = "/api/training-groups";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value(JwtKeysConfiguration.ISSUER_PROPERTY)
    private String issuer;

    private record TestUser(UUID id, String username, String accessToken) {
    }

    private TestUser userWith(Set<Authority> stored, List<String> staleTokenAuthorities) {
        String username = "ZBM" + UUID.randomUUID().toString().substring(0, 4);
        UUID id = userService.createActiveUser(username, "{noop}unused", stored).uuid();
        return new TestUser(id, username, accessTokenFor(id, username, staleTokenAuthorities));
    }

    private String accessTokenFor(UUID userId, String username, List<String> authoritiesClaim) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .claim("user_id", userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
        if (authoritiesClaim != null) {
            claims.claim("authorities", authoritiesClaim);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
    }

    private ResultActions listTrainingGroups(TestUser user) throws Exception {
        return mockMvc.perform(get(TRAINING_GROUPS)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(MediaType.APPLICATION_JSON));
    }

    private void adminSetsPermissions(TestUser admin, TestUser target, String... authorities) throws Exception {
        String body = "{\"authorities\":[" + String.join(",", java.util.Arrays.stream(authorities)
                .map(a -> "\"" + a + "\"").toList()) + "]}";
        mockMvc.perform(put("/api/users/{id}/permissions", target.id())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    private TestUser admin() {
        return userWith(Set.of(Authority.MEMBERS_PERMISSIONS), null);
    }

    @Test
    @DisplayName("revoking GROUPS:TRAINING closes the training groups API for the same access token")
    void revokedPermissionStopsWorkingWithoutNewToken() throws Exception {
        TestUser admin = admin();
        TestUser user = userWith(Set.of(Authority.GROUPS_TRAINING), null);
        listTrainingGroups(user).andExpect(status().isOk());

        adminSetsPermissions(admin, user);

        listTrainingGroups(user).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("granting GROUPS:TRAINING opens the training groups API for the same access token")
    void grantedPermissionStartsWorkingWithoutNewToken() throws Exception {
        TestUser admin = admin();
        TestUser user = userWith(Set.of(), null);
        listTrainingGroups(user).andExpect(status().isForbidden());

        adminSetsPermissions(admin, user, Authority.GROUPS_TRAINING.getValue());

        listTrainingGroups(user).andExpect(status().isOk());
    }

    @Test
    @DisplayName("an authorities claim in a user token is not trusted")
    void authoritiesClaimOfUserTokenIsIgnored() throws Exception {
        TestUser user = userWith(Set.of(), List.of(Authority.GROUPS_TRAINING.getValue()));

        listTrainingGroups(user).andExpect(status().isForbidden());
    }
}
