package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.MemberRegistrationBlockRepository;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRegistrationSanctionService")
class MemberRegistrationSanctionServiceTest {

    private static final MemberId SANCTIONED_MEMBER = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId OTHER_MEMBER = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MemberRegistrationBlockRepository blockRepository;

    private MemberRegistrationSanctionService service;

    @BeforeEach
    void setUp() {
        service = new MemberRegistrationSanctionService(eventRepository, blockRepository);
    }

    @Nested
    @DisplayName("applyMissedSelectionSanction")
    class ApplyMissedSelectionSanction {

        @Test
        @DisplayName("should block the member via block repository")
        void shouldBlockMember() {
            when(eventRepository.findAll(any(), any(Pageable.class))).thenReturn(Page.empty());

            service.applyMissedSelectionSanction(SANCTIONED_MEMBER);

            verify(blockRepository).block(SANCTIONED_MEMBER);
        }

        @Test
        @DisplayName("should unregister member from events with open registrations")
        void shouldUnregisterMemberFromEventsWithOpenRegistrations() {
            EventId eventId = EventId.generate();
            Event activeEvent = EventTestDataBuilder.anEventWithId(eventId)
                    .withDate(LocalDate.now().plusDays(30))
                    .withRegistrationDeadline(LocalDate.now().plusDays(7))
                    .build();
            activeEvent.publish();
            activeEvent.registerMember(SANCTIONED_MEMBER, SiCardNumber.of("1234567"), null);

            when(eventRepository.findAll(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeEvent)));

            service.applyMissedSelectionSanction(SANCTIONED_MEMBER);

            ArgumentCaptor<Event> savedCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(savedCaptor.capture());
            Event savedEvent = savedCaptor.getValue();
            assertThat(savedEvent.findRegistration(SANCTIONED_MEMBER)).isEmpty();
        }

        @Test
        @DisplayName("should not unregister other members from events with open registrations")
        void shouldNotUnregisterOtherMembers() {
            EventId eventId = EventId.generate();
            Event activeEvent = EventTestDataBuilder.anEventWithId(eventId)
                    .withDate(LocalDate.now().plusDays(30))
                    .withRegistrationDeadline(LocalDate.now().plusDays(7))
                    .build();
            activeEvent.publish();
            activeEvent.registerMember(OTHER_MEMBER, SiCardNumber.of("1234567"), null);

            when(eventRepository.findAll(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeEvent)));

            service.applyMissedSelectionSanction(SANCTIONED_MEMBER);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not unregister member from events with closed registrations")
        void shouldNotUnregisterFromClosedRegistrations() {
            EventId eventId = EventId.generate();
            Event pastDeadlineEvent = EventTestDataBuilder.anEventWithId(eventId)
                    .withDate(LocalDate.now().plusDays(30))
                    .withRegistrationDeadline(LocalDate.now().minusDays(1))
                    .build();
            pastDeadlineEvent.publish();

            when(eventRepository.findAll(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(pastDeadlineEvent)));

            service.applyMissedSelectionSanction(SANCTIONED_MEMBER);

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isMemberBlocked")
    class IsMemberBlocked {

        @Test
        @DisplayName("should delegate to block repository")
        void shouldDelegateToBlockRepository() {
            when(blockRepository.isBlocked(SANCTIONED_MEMBER)).thenReturn(true);

            assertThat(service.isMemberBlocked(SANCTIONED_MEMBER)).isTrue();
        }

        @Test
        @DisplayName("should return false when block repository reports not blocked")
        void shouldReturnFalseWhenNotBlocked() {
            when(blockRepository.isBlocked(SANCTIONED_MEMBER)).thenReturn(false);

            assertThat(service.isMemberBlocked(SANCTIONED_MEMBER)).isFalse();
        }
    }
}
