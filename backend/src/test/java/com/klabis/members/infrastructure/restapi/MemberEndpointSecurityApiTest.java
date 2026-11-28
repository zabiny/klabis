package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.MemberViewAccess;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.MemberFilter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Endpoint security (401 / 403)")
@MembersWebMvcTest
class MemberEndpointSecurityApiTest extends MemberApiTestBase {

    @Autowired
    private RegistrationPort registrationService;

    private static final UUID SOME_MEMBER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private static final String REGISTER_BODY = """
            {
                "firstName": "Jan",
                "lastName": "Novák",
                "dateOfBirth": "2005-05-15",
                "nationality": "CZ",
                "gender": "MALE",
                "email": "jan@example.com",
                "phone": "+420777123456",
                "birthNumber": "050515/1234",
                "address": {
                    "street": "Hlavní 123",
                    "city": "Praha",
                    "postalCode": "11000",
                    "country": "CZ"
                }
            }
            """;

    private static final String SUSPEND_BODY = """
            {
                "reason": "ODHLASKA",
                "note": "Test termination"
            }
            """;

    private record SecuredEndpoint(String name, HttpMethod method, String url, String body, Authority insufficient) {
    }

    private static final List<SecuredEndpoint> SECURED_ENDPOINTS = List.of(
            new SecuredEndpoint("GET /api/members/{id}", HttpMethod.GET,
                    "/api/members/" + SOME_MEMBER_ID, null, Authority.MEMBERS_MANAGE),
            new SecuredEndpoint("GET /api/members", HttpMethod.GET,
                    "/api/members", null, Authority.CALENDAR_MANAGE),
            new SecuredEndpoint("GET /api/members/options", HttpMethod.GET,
                    "/api/members/options", null, Authority.MEMBERS_MANAGE),
            new SecuredEndpoint("POST /api/members", HttpMethod.POST,
                    "/api/members", REGISTER_BODY, Authority.MEMBERS_READ),
            new SecuredEndpoint("POST /api/members/{id}/suspend", HttpMethod.POST,
                    "/api/members/" + SOME_MEMBER_ID + "/suspend", SUSPEND_BODY, Authority.MEMBERS_READ),
            new SecuredEndpoint("POST /api/members/{id}/resume", HttpMethod.POST,
                    "/api/members/" + SOME_MEMBER_ID + "/resume", null, Authority.MEMBERS_READ));

    static Stream<Arguments> unauthenticatedRequests() {
        return SECURED_ENDPOINTS.stream()
                .map(e -> argumentSet(e.name(), e.method(), e.url(), e.body()));
    }

    static Stream<Arguments> requestsWithInsufficientAuthority() {
        return SECURED_ENDPOINTS.stream()
                .map(e -> argumentSet(e.name(), e.method(), e.url(), e.body(), e.insufficient()));
    }

    private static MockHttpServletRequestBuilder requestTo(HttpMethod method, String url, String body) {
        MockHttpServletRequestBuilder request = request(method, url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        return body == null ? request : request.content(body);
    }

    @ParameterizedTest
    @MethodSource("unauthenticatedRequests")
    @DisplayName("unauthenticated request is answered with 401")
    void shouldReturn401WhenUnauthenticated(HttpMethod method, String url, String body) throws Exception {
        mockMvc.perform(requestTo(method, url, body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @MethodSource("requestsWithInsufficientAuthority")
    @DisplayName("request with insufficient authority is answered with 403 and reaches no service")
    void shouldReturn403WhenAuthorityIsInsufficient(HttpMethod method, String url, String body, Authority held)
            throws Exception {
        mockMvc.perform(requestTo(method, url, body)
                        .with(klabisAuthentication(JwtParams.jwtTokenParams("ZBM0102", UUID.randomUUID())
                                .withAuthorities(held))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));

        Mockito.verifyNoInteractions(managementService, registrationService);
    }
}
