package com.klabis.events.infrastructure.scheduler;

import com.klabis.events.application.EventManagementPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventCompletionScheduler")
class EventCompletionSchedulerTest {

    @Mock
    private EventManagementPort eventManagementPort;

    @InjectMocks
    private EventCompletionScheduler scheduler;

    @Test
    @DisplayName("should delegate to EventManagementPort.finishExpiredActiveEvents() with given date")
    void shouldDelegateToPortWithGivenDate() {
        LocalDate date = LocalDate.of(2025, 2, 1);

        scheduler.completeExpiredEvents(date);

        verify(eventManagementPort).finishExpiredActiveEvents(date);
    }

    @Test
    @DisplayName("should delegate to EventManagementPort.finishExpiredActiveEvents() with today when called without parameters")
    void shouldDelegateToPortWithCurrentDate() {
        scheduler.completeExpiredEvents();

        verify(eventManagementPort).finishExpiredActiveEvents(any(LocalDate.class));
    }
}
