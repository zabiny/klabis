package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.sync.SyncWebMvcTest;
import com.klabis.sync.application.RecordView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SynchronizationController - records needing attention")
@SyncWebMvcTest
class SyncRecordsControllerTest {

    private static final Instant LAST_SUCCESS = Instant.parse("2026-02-03T10:15:30Z");
    private static final String URL = "/api/sync/records?status=CONFLICT&status=FAILED";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SyncHistoryQuery historyQuery;

    @BeforeEach
    void setUp() {
        when(historyQuery.listRecordsIn(any())).thenReturn(List.of());
    }

    private RecordView record(String entityId, SyncStatus status, Instant lastSuccess, int failures) {
        return new RecordView(new SyncTarget(SyncEntityType.EVENT, entityId), "ext-" + entityId, status, lastSuccess, failures);
    }

    @Test
    @DisplayName("returns record fields, label from the describer and syncState and entity links")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void returnsRecordFields() throws Exception {
        when(historyQuery.listRecordsIn(any())).thenReturn(List.of(record("event-1", SyncStatus.FAILED, LAST_SUCCESS, 3)));

        mockMvc.perform(get(URL).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.syncRecords[0].entityType").value("events"))
                .andExpect(jsonPath("$._embedded.syncRecords[0].status").value("FAILED"))
                .andExpect(jsonPath("$._embedded.syncRecords[0].entityLabel").value("Spring Cup"))
                .andExpect(jsonPath("$._embedded.syncRecords[0].externalId").value("ext-event-1"))
                .andExpect(jsonPath("$._embedded.syncRecords[0].lastSuccessfulSyncAt").exists())
                .andExpect(jsonPath("$._embedded.syncRecords[0].failedAttemptsSinceLastSuccess").value(3))
                .andExpect(jsonPath("$._embedded.syncRecords[0]._links.syncState.href")
                        .value(containsString("/api/events/event-1/sync")))
                .andExpect(jsonPath("$._embedded.syncRecords[0]._links.entity.href")
                        .value(containsString("/api/events/event-1")))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/api/sync/records")));
    }

    @Test
    @DisplayName("falls back to the external id without entity link and omits a never-synchronised timestamp")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void fallsBackToExternalId() throws Exception {
        when(historyQuery.listRecordsIn(any())).thenReturn(List.of(record("gone", SyncStatus.CONFLICT, null, 0)));

        mockMvc.perform(get(URL).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.syncRecords[0].entityLabel").value("ext-gone"))
                .andExpect(jsonPath("$._embedded.syncRecords[0].lastSuccessfulSyncAt").doesNotExist())
                .andExpect(jsonPath("$._embedded.syncRecords[0]._links.entity").doesNotExist());
    }

    @Test
    @DisplayName("passes the repeated status parameter to the query")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void passesStatuses() throws Exception {
        mockMvc.perform(get(URL).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(status().isOk());

        ArgumentCaptor<Set<SyncStatus>> statuses = ArgumentCaptor.captor();
        verify(historyQuery).listRecordsIn(statuses.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(SyncStatus.CONFLICT, SyncStatus.FAILED);
    }

    @Test
    @DisplayName("answers 400 for a missing or unknown status")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void rejectsInvalidStatus() throws Exception {
        for (String url : List.of("/api/sync/records", "/api/sync/records?status=NOPE")) {
            mockMvc.perform(get(url).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("answers 403 without SYNC:MANAGE")
    @WithKlabisMockUser(authorities = {})
    void forbiddenWithoutAuthority() throws Exception {
        mockMvc.perform(get(URL).accept(MediaTypes.HAL_FORMS_JSON)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("attempt list links to the records needing attention")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void attemptListLinksToAttention() throws Exception {
        when(historyQuery.listAttempts(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/sync/attempts").accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.recordsNeedingAttention.href")
                        .value(containsString("/api/sync/records?status=CONFLICT&status=FAILED")));
    }
}
