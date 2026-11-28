package com.klabis.members.infrastructure.restapi;

import com.jayway.jsonpath.JsonPath;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.domain.MemberFilter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/members — filter params")
@MembersWebMvcTest
class MemberListFilterApiTest extends MemberApiTestBase {

    @Test
    @DisplayName("3.1 — q param is passed as fulltextQuery in the filter")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldPassQParamAsFulltextQuery() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/members")
                .param("q", "novak")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));

        Mockito.verify(managementService).listMembers(
                argThat(filter -> "novak".equals(filter.fulltextQuery())),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("3.2 — q param absent means fulltextQuery is null in filter")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldPassNullFulltextQueryWhenQParamAbsent() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/members")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));

        Mockito.verify(managementService).listMembers(
                argThat(filter -> filter.fulltextQuery() == null),
                any(Pageable.class)
        );
    }

    static Stream<Arguments> manageCallerStatuses() {
        return Stream.of(
                arguments(null, MemberFilter.StatusFilter.ACTIVE),
                arguments("ACTIVE", MemberFilter.StatusFilter.ACTIVE),
                arguments("INACTIVE", MemberFilter.StatusFilter.INACTIVE),
                arguments("ALL", MemberFilter.StatusFilter.ALL));
    }

    @ParameterizedTest(name = "MANAGE caller sending status={0} gets {1} in the filter")
    @MethodSource("manageCallerStatuses")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void manageCallerStatusReachesService(String status, MemberFilter.StatusFilter expected) throws Exception {
        assertStatusReachingService(status, expected);
    }

    static Stream<Arguments> regularCallerStatuses() {
        return Stream.of(arguments((String) null), arguments("INACTIVE"), arguments("ALL"));
    }

    @ParameterizedTest(name = "caller without MANAGE sending status={0} is silently forced to ACTIVE")
    @MethodSource("regularCallerStatuses")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void regularCallerIsForcedToActive(String status) throws Exception {
        assertStatusReachingService(status, MemberFilter.StatusFilter.ACTIVE);
    }

    private void assertStatusReachingService(String status, MemberFilter.StatusFilter expected) throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var request = get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        if (status != null) {
            request.param("status", status);
        }
        mockMvc.perform(request).andExpect(status().isOk());

        ArgumentCaptor<MemberFilter> filter = ArgumentCaptor.forClass(MemberFilter.class);
        verify(managementService).listMembers(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().status()).isEqualTo(expected);
    }

    static Stream<Arguments> selfLinkQueries() {
        return Stream.of(
                arguments(Named.of("status and q", "status=ACTIVE&q=novak"), List.of("status=ACTIVE", "q=novak")),
                arguments(Named.of("only status", "status=ACTIVE"), List.of("status=ACTIVE")),
                arguments(Named.of("only q", "q=novak"), List.of("q=novak")),
                arguments(Named.of("incomplete", "incomplete=true"), List.of("incomplete=true")));
    }

    @ParameterizedTest(name = "self link keeps applied filter params as a concrete URL: {0}")
    @MethodSource("selfLinkQueries")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void selfLinkIsConcreteUrlWithAppliedFilterParams(String query, List<String> expectedParams) throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var response = mockMvc.perform(get("/api/members?" + query).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String selfHref = JsonPath.read(response, "$._links.self.href");
        assertThat(selfHref).contains(expectedParams).doesNotContain("{", "}", "%7B", "%7D");
    }

    @Test
    @DisplayName("3.3 — unrecognized status value returns 400")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void shouldReturn400ForUnrecognizedStatusValue() throws Exception {
        mockMvc.perform(get("/api/members")
                .param("status", "INVALID_VALUE")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7.3 — incomplete=true filters for MEMBERS:MANAGE callers")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void incompleteTrueFiltersForManageCaller() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/members")
                .param("incomplete", "true")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk());

        Mockito.verify(managementService).listMembers(
                argThat(MemberFilter::incompleteOnly),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("7.3 — incomplete=true is ignored for callers without MEMBERS:MANAGE")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void incompleteTrueIsIgnoredForNonManageCaller() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/members")
                .param("incomplete", "true")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk());

        Mockito.verify(managementService).listMembers(
                argThat(filter -> !filter.incompleteOnly()),
                any(Pageable.class)
        );
    }

}
