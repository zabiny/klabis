package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.GuardianInformation;
import com.klabis.members.domain.Member;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * End-to-end scenarios for tasks.md 12.2: what {@link Member#syncFromOris} and the
 * engine's own change-detection protect (design.md D3, D6). Mirrors
 * {@code OrisEventSyncScenarioIntegrationTest}'s "field ownership" and "conflict
 * lifecycle" nested scenarios, adapted for the members module.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Members on the sync engine — Klabis-owned field protection")
class MemberOrisSyncProtectionIntegrationTest {

    @Autowired
    private OrisClubKeyPort orisClubKeyPort;

    @Autowired
    private MemberDiscoveryJob memberDiscoveryJob;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private ManagementPort managementPort;

    @MockitoBean
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(744_000);

    private int orisId;
    private String regNum;
    private SyncRecord enrolled;
    private MemberId memberId;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        regNum = "ZBM%04d".formatted(orisId % 10000);
        orisClubKeyPort.store("test-club-key");

        stubClubMembers(orisClubMember(orisId, regNum, "Jan", "Novák", "0"));
        memberDiscoveryJob.discoverNewMembers();
        enrolled = enrolledRecord();
        memberId = memberIdOf(enrolled);
    }

    @Test
    @DisplayName("a Klabis-only field edited by an administrator survives a subsequent sync pass untouched")
    void klabisOwnedFieldSurvivesSubsequentSyncPass() {
        GuardianInformation guardian = new GuardianInformation(
                "Petr", "Novák", "PARENT",
                com.klabis.members.domain.EmailAddress.of("petr@example.com"),
                com.klabis.members.domain.PhoneNumber.of("+420111111111"));
        Member.UpdateMember baseline = managementPort.prefilledUpdateCommand(memberId);
        managementPort.updateMember(memberId, new Member.UpdateMember(
                baseline.email(), baseline.phone(), baseline.address(), baseline.chipNumber(),
                baseline.nationality(), baseline.bankAccountNumber(), baseline.identityCard(),
                baseline.drivingLicenseGroup(), baseline.medicalCourse(), baseline.trainerLicense(),
                baseline.refereeLicense(), baseline.dietaryRestrictions(), guardian,
                baseline.firstName(), baseline.lastName(), baseline.dateOfBirth(), baseline.gender(),
                baseline.birthNumber(), null));

        // Same ORIS payload as discovery compared against — the guardian edit is the
        // only local change, and it must not be visible to the projection at all.
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        Member stillGuarded = managementPort.getMember(memberId);
        assertThat(stillGuarded.getGuardian()).isEqualTo(guardian);
    }

    @Test
    @DisplayName("a chip number entered in Klabis while ORIS holds none becomes a conflict, not a silent clear")
    void chipNumberEnteredInKlabisBecomesConflictWhenOrisHoldsNone() {
        Member.UpdateMember baseline = managementPort.prefilledUpdateCommand(memberId);
        managementPort.updateMember(memberId, new Member.UpdateMember(
                baseline.email(), baseline.phone(), baseline.address(), "998877",
                baseline.nationality(), baseline.bankAccountNumber(), baseline.identityCard(),
                baseline.drivingLicenseGroup(), baseline.medicalCourse(), baseline.trainerLicense(),
                baseline.refereeLicense(), baseline.dietaryRestrictions(), baseline.guardian(),
                baseline.firstName(), baseline.lastName(), baseline.dateOfBirth(), baseline.gender(),
                baseline.birthNumber(), null));

        // ORIS still reports si == 0 (no chip) — unchanged from discovery's payload.
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.CONFLICT);
        Member stillHasChip = managementPort.getMember(memberId);
        assertThat(stillHasChip.getChipNumber()).isEqualTo("998877");
    }

    private SyncRecord enrolledRecord() {
        SyncedEntityReference reference = synchronizationPort.findByExternalReferences(
                        SyncEntityType.MEMBER, ExternalSystem.ORIS, List.of(String.valueOf(orisId)))
                .stream()
                .findFirst()
                .orElseThrow();
        return synchronizationPort.findByTarget(reference.target()).orElseThrow();
    }

    private static MemberId memberIdOf(SyncRecord record) {
        return new MemberId(UUID.fromString(record.getTarget().entityId()));
    }

    private static ClubMember orisClubMember(int id, String regNum, String firstName, String lastName, String si) {
        return ClubMemberBuilder.builder()
                .id(id)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user" + id)
                .firstName(firstName)
                .lastName(lastName)
                .email(regNum.toLowerCase() + "@example.com")
                .street("Testovací 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .birthday(LocalDate.of(1990, 1, 15))
                .phone("700000001")
                .gender("M")
                .persNum("900115/0000")
                .nationality("CZ")
                .si(si)
                .build();
    }

    private void stubClubMembers(ClubMember clubMember) {
        when(orisApiClient.getClubUserList("test-club-key"))
                .thenReturn(new OrisApiClient.OrisResponse<>(
                        Map.of(String.valueOf(clubMember.id()), clubMember), "JSON", "OK", null, "getClubUserList"));
    }
}
