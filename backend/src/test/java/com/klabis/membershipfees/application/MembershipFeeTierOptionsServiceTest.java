package com.klabis.membershipfees.application;

import com.klabis.common.ui.HalFormsInlineOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipFeeTierOptionsService")
class MembershipFeeTierOptionsServiceTest {

    @Mock
    private RankingOptionsPort rankingOptionsPort;
    @Mock
    private EventTypeOptionsPort eventTypeOptionsPort;

    private MembershipFeeTierOptionsService testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new MembershipFeeTierOptionsService(rankingOptionsPort, eventTypeOptionsPort);
    }

    @Test
    @DisplayName("should delegate ranking options to the ranking options port")
    void shouldDelegateRankingOptions() {
        List<HalFormsInlineOption> options = List.of(new HalFormsInlineOption("A", "Žebříček A"));
        when(rankingOptionsPort.listRankingOptions()).thenReturn(options);

        assertThat(testedSubject.listRankingOptions()).isSameAs(options);
    }

    @Test
    @DisplayName("should delegate event type options to the event type options port")
    void shouldDelegateEventTypeOptions() {
        List<HalFormsInlineOption> options = List.of(new HalFormsInlineOption("id", "Závod"));
        when(eventTypeOptionsPort.listEventTypeOptions()).thenReturn(options);

        assertThat(testedSubject.listEventTypeOptions()).isSameAs(options);
    }
}
