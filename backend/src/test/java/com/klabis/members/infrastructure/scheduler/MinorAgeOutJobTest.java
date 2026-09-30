package com.klabis.members.infrastructure.scheduler;

import com.klabis.members.application.MemberAgeOutPort;
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
@DisplayName("MinorAgeOutJob")
class MinorAgeOutJobTest {

    @Mock
    private MemberAgeOutPort memberAgeOutPort;

    @InjectMocks
    private MinorAgeOutJob job;

    @Test
    @DisplayName("should delegate to port with given date")
    void shouldDelegateWithGivenDate() {
        LocalDate date = LocalDate.of(2026, 3, 1);

        job.processMembersComingOfAge(date);

        verify(memberAgeOutPort).processMembersComingOfAge(date);
    }

    @Test
    @DisplayName("should delegate to port with today when called without parameters")
    void shouldDelegateWithToday() {
        job.processMembersComingOfAge();

        verify(memberAgeOutPort).processMembersComingOfAge(any(LocalDate.class));
    }
}
