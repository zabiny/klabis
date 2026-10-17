package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.User;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.SyncWebMvcTest;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SynchronizationController - attempts feed")
@SyncWebMvcTest
class SyncAttemptsControllerTest {

    private static final Instant STARTED_AT = Instant.parse("2026-02-03T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SyncHistoryQuery historyQuery;

    @Autowired
    private UserService userService;

    private final UserId actingUserId = UserId.newId();

    @BeforeEach
    void setUp() {
        when(historyQuery.listAttempts(any(), any())).thenReturn(Page.empty());
    }

    private AttemptView view(SyncTriggerKind trigger, SyncDirection direction, SyncOutcome outcome,
                             String failureReason, String actingUser) {
        SyncAttempt attempt = SyncAttempt.record(SyncRecordId.newId(), STARTED_AT, trigger, direction, outcome,
                SyncHash.of("h"), SyncHash.of("h"), failureReason, actingUser);
        return new AttemptView(attempt, new SyncTarget(SyncEntityType.EVENT, "event-1"), "8123");
    }

    @Test
    @DisplayName("returns attempt fields, resolved username and the syncState link")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void returnsAttemptFields() throws Exception {
        AttemptView manual = view(SyncTriggerKind.MANUAL, SyncDirection.INWARD, SyncOutcome.FAILED, "boom",
                actingUserId.uuid().toString());
        when(historyQuery.listAttempts(any(), any())).thenReturn(new PageImpl<>(List.of(manual), PageRequest.of(0, 25), 1));
        when(userService.findUserById(actingUserId)).thenReturn(Optional.of(User.createdUser("ZBM9000")));

        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].startedAt").exists())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].trigger").value("MANUAL"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].direction").value("INWARD"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].outcome").value("FAILED"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].failureReason").value("boom"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].performedBy").value("ZBM9000"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].entityType").value("events"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].entityLabel").value("8123"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0].externalId").value("8123"))
                .andExpect(jsonPath("$._embedded.syncAttempts[0]._links.syncState.href")
                        .value(containsString("/api/events/event-1/sync")))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("omits direction, failureReason and performedBy when absent, and when the user is unknown")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void omitsAbsentFields() throws Exception {
        AttemptView unknownUser = view(SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS, null,
                actingUserId.uuid().toString());
        AttemptView automatic = view(SyncTriggerKind.SCHEDULED, null, SyncOutcome.OUTAGE, null, null);
        when(historyQuery.listAttempts(any(), any()))
                .thenReturn(new PageImpl<>(List.of(unknownUser, automatic), PageRequest.of(0, 25), 2));
        when(userService.findUserById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].direction").doesNotExist())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].failureReason").doesNotExist())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].performedBy").doesNotExist())
                .andExpect(jsonPath("$._embedded.syncAttempts[1].performedBy").doesNotExist());
    }

    @Test
    @DisplayName("treats an acting user that is not a UUID as unknown instead of failing the page")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void nonUuidActingUserIsUnknown() throws Exception {
        AttemptView legacy = view(SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS, null, "not-a-uuid");
        when(historyQuery.listAttempts(any(), any())).thenReturn(new PageImpl<>(List.of(legacy), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.syncAttempts[0].performedBy").doesNotExist());
    }

    @Test
    @DisplayName("hides attempts that changed nothing by default and passes includeUnchanged and paging to the query")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void passesFilterAndPaging() throws Exception {
        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/sync/attempts?includeUnchanged=true&page=2&size=5").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<AttemptFilter> filter = ArgumentCaptor.forClass(AttemptFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(historyQuery, org.mockito.Mockito.times(2)).listAttempts(filter.capture(), pageable.capture());
        assertThat(filter.getAllValues()).containsExactly(AttemptFilter.changedOnly(), AttemptFilter.all());
        assertThat(pageable.getAllValues().get(0).getPageSize()).isEqualTo(25);
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(pageable.getAllValues().get(1).getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("is forbidden without SYNC:MANAGE")
    @WithKlabisMockUser(authorities = {})
    void forbiddenWithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("root exposes syncOverview to SYNC:MANAGE")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void rootLinkPresentForManager() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.syncOverview.href").value(containsString("/api/sync/attempts")));
    }

    @Test
    @DisplayName("root hides syncOverview from other users")
    @WithKlabisMockUser(authorities = {})
    void rootLinkAbsentWithoutAuthority() throws Exception {
        mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.syncOverview").doesNotExist());
    }
}
