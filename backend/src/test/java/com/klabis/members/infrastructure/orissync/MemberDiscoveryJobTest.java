package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDiscoveryJob")
class MemberDiscoveryJobTest {

    @Mock
    private OrisClubMembers orisClubMembers;

    @Mock
    private SynchronizationPort synchronizationPort;

    private MemberDiscoveryJob job;

    @BeforeEach
    void setUp() {
        job = new MemberDiscoveryJob(orisClubMembers, synchronizationPort);
    }

    @Test
    @DisplayName("enrols only ORIS members not already paired, leaving already-discovered ones untouched")
    void enrolsOnlyUndiscoveredMembers() {
        stubOrisMembers(clubMember(1, "ZBM0001", true), clubMember(2, "ZBM0002", true), clubMember(3, "ZBM0003", true));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.MEMBER, "existing-member-id"),
                        new ExternalReference(ExternalSystem.ORIS, "2"))));

        job.discoverNewMembers();

        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, "1"), null);
        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, "3"), null);
        verify(synchronizationPort, never()).pullAndEnroll(
                eq(SyncEntityType.MEMBER), eq(new ExternalReference(ExternalSystem.ORIS, "2")), any());
        verify(synchronizationPort, times(2)).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("enrols nothing when every ORIS member is already paired")
    void enrolsNothingWhenAllMembersAlreadyPaired() {
        stubOrisMembers(clubMember(1, "ZBM0001", true));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.MEMBER, "existing-member-id"),
                        new ExternalReference(ExternalSystem.ORIS, "1"))));

        job.discoverNewMembers();

        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("does nothing when ORIS reports no club members")
    void doesNothingWhenOrisReportsNoMembers() {
        stubOrisMembers();

        job.discoverNewMembers();

        verify(synchronizationPort, never()).findByExternalReferences(any(), any(), any());
        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("never enrols a new member whose ORIS club membership has lapsed (D5)")
    void skipsNewMemberWithLapsedMembership() {
        stubOrisMembers(clubMember(1, "ZBM0001", false), clubMember(2, "ZBM0002", true));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of());

        job.discoverNewMembers();

        verify(synchronizationPort, never()).pullAndEnroll(
                eq(SyncEntityType.MEMBER), eq(new ExternalReference(ExternalSystem.ORIS, "1")), any());
        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, "2"), null);
    }

    @Test
    @DisplayName("never checks pairing for a lapsed member — the validity filter runs before pairing is even considered (D5)")
    void neverChecksPairingForALapsedMember() {
        stubOrisMembers(clubMember(1, "ZBM0001", false));

        job.discoverNewMembers();

        verify(synchronizationPort, never()).findByExternalReferences(any(), any(), any());
        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("does nothing and makes no ORIS call when no club key is held (D9)")
    void doesNothingWhenNoClubKeyHeld() {
        when(orisClubMembers.listClubMembers()).thenThrow(new ClubKeyNotSetException());

        job.discoverNewMembers();

        verify(synchronizationPort, never()).findByExternalReferences(any(), any(), any());
        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("one member's enrolment failure does not stop the rest of the run")
    void oneMemberFailureDoesNotStopTheRest() {
        stubOrisMembers(clubMember(1, "ZBM0001", true), clubMember(2, "ZBM0002", true));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.MEMBER), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of());
        when(synchronizationPort.pullAndEnroll(
                eq(SyncEntityType.MEMBER), eq(new ExternalReference(ExternalSystem.ORIS, "1")), any()))
                .thenThrow(new RuntimeException("boom"));

        job.discoverNewMembers();

        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, "1"), null);
        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, "2"), null);
    }

    private static ClubMember clubMember(int id, String regNum, boolean valid) {
        return ClubMemberBuilder.builder()
                .id(id)
                .regNum(regNum)
                .valid(valid)
                .firstName("Jan")
                .lastName("Novak")
                .birthday(LocalDate.of(1990, 1, 15))
                .gender("M")
                .nationality("CZ")
                .persNum("900115/0000")
                .email("jan@example.com")
                .street("Testovaci 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .si(0)
                .build();
    }

    private void stubOrisMembers(ClubMember... members) {
        Map<String, ClubMember> byId = new LinkedHashMap<>();
        for (ClubMember member : members) {
            byId.put(String.valueOf(member.id()), member);
        }
        when(orisClubMembers.listClubMembers()).thenReturn(byId);
    }
}
