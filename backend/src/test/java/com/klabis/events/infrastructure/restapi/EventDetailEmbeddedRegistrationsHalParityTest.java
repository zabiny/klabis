package com.klabis.events.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.EventsWebMvcTest;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRegistrationCreateEventRegistrationBuilder;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.sync.application.SynchronizationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Event detail HAL parity (_embedded registrations)")
@EventsWebMvcTest
class EventDetailEmbeddedRegistrationsHalParityTest {

    private static final UUID MEMBER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventManagementPort eventManagement;

    @Autowired
    private EventRegistrationPort eventRegistration;

    @Autowired
    private Members members;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @BeforeEach
    void stubSynchronizationPortAbsent() {
        when(synchronizationPort.findByTarget(any())).thenReturn(Optional.empty());
        when(synchronizationPort.findActiveByTargets(any(), any())).thenReturn(List.of());
    }

    private String getEventBody() throws Exception {
        MemberId memberId = new MemberId(MEMBER_UUID);
        Event event = EventTestDataBuilder.anEvent().buildPublished();
        EventRegistration registration = EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                .memberId(memberId).siCardNumber(new SiCardNumber("12345")).build());
        when(eventManagement.getEvent(any(), anyBoolean())).thenReturn(event);
        when(eventRegistration.listRegistrations(any())).thenReturn(List.of(registration));
        when(members.findByIds(any())).thenReturn(Map.of(memberId, new MemberDto(MEMBER_UUID, "Jan", "Novak", "jan@example.com")));
        return mockMvc.perform(get("/api/events/{id}", UUID.randomUUID()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("registration port and members are not called when a plain JSON response is rendered")
    @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_READ})
    void plainJsonDoesNotLoadRegistrations() throws Exception {
        when(eventManagement.getEvent(any(), anyBoolean())).thenReturn(EventTestDataBuilder.anEvent().buildPublished());

        mockMvc.perform(get("/api/events/{id}", UUID.randomUUID()).accept("application/json"))
                .andExpect(status().isOk());

        verifyNoInteractions(eventRegistration, members);
    }

    @Test
    @DisplayName("registration port is called exactly once when a HAL response is rendered")
    @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_READ})
    void halLoadsRegistrationsOnce() throws Exception {
        getEventBody();

        verify(eventRegistration, times(1)).listRegistrations(any());
    }

    @Test
    @DisplayName("events reader: embedded registration rows")
    @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_READ})
    void readerView() throws Exception {
        assertThat(HalShape.of(getEventBody(), "$._embedded.registrationDtoList[0]")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("finance manager: embedded registration rows")
    @WithKlabisMockUser(username = "admin", authorities = {Authority.EVENTS_READ, Authority.FINANCE_MANAGE, Authority.MEMBERS_READ})
    void financeManagerView() throws Exception {
        assertThat(HalShape.of(getEventBody(), "$._embedded.registrationDtoList[0]")).isEqualTo(List.of());
    }
}
