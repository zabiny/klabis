package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
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
 * End-to-end scenario for tasks.md 12.1: a club key is supplied, discovery brings in a
 * valid ORIS member, the member appears in Klabis with the ORIS registration number,
 * and a later ORIS change reaches the member record — mirroring
 * {@link com.klabis.events.infrastructure.orissync.OrisEventSyncScenarioIntegrationTest}'s
 * shape for the members module (design.md D1, D4, D6, D7).
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Members on the sync engine — end-to-end discovery scenario")
class MemberOrisSyncScenarioIntegrationTest {

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

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(733_000);

    private int orisId;
    private String regNum;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        regNum = "ZBM%04d".formatted(orisId % 10000);
        orisClubKeyPort.store("test-club-key");
    }

    @Test
    @DisplayName("discovery brings a valid ORIS member into Klabis with the ORIS registration number")
    void discoveryBringsMemberIntoKlabisWithOrisRegistrationNumber() {
        stubClubMembers(orisClubMember(orisId, regNum, "Jan", "Novák", "700000001"));

        memberDiscoveryJob.discoverNewMembers();

        SyncRecord record = enrolledRecord();
        assertThat(record.getExternalReference())
                .isEqualTo(new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        Member imported = managementPort.getMember(memberIdOf(record));
        assertThat(imported.getRegistrationNumber().getValue()).isEqualTo(regNum);
        assertThat(imported.getFirstName()).isEqualTo("Jan");
        assertThat(imported.getLastName()).isEqualTo("Novák");
    }

    @Test
    @DisplayName("no ORIS call and nothing enrolled when no club key is held")
    void noClubKeyMeansNoOrisCallAndNothingEnrolled() {
        orisClubKeyPort.clear();
        stubClubMembers(orisClubMember(orisId, regNum, "Jan", "Novák", "700000001"));

        memberDiscoveryJob.discoverNewMembers();

        assertThat(synchronizationPort.findByExternalReferences(
                SyncEntityType.MEMBER, ExternalSystem.ORIS, List.of(String.valueOf(orisId))))
                .isEmpty();
    }

    @Test
    @DisplayName("a later ORIS change to a paired member reaches the member record on the next sync pass")
    void laterOrisChangeReachesMemberRecord() {
        stubClubMembers(orisClubMember(orisId, regNum, "Jan", "Novák", "700000001"));
        memberDiscoveryJob.discoverNewMembers();
        SyncRecord enrolled = enrolledRecord();

        stubClubMembers(orisClubMember(orisId, regNum, "Jan", "Novák", "700000002"));
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        Member updated = managementPort.getMember(memberIdOf(enrolled));
        assertThat(updated.getPhone().value()).isEqualTo("+420700000002");
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

    private static ClubMember orisClubMember(int id, String regNum, String firstName, String lastName, String phone) {
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
                .phone(phone)
                .gender("M")
                .persNum("900115/0000")
                .nationality("CZ")
                .si("0")
                .build();
    }

    private void stubClubMembers(ClubMember clubMember) {
        when(orisApiClient.getClubUserList("test-club-key"))
                .thenReturn(new OrisApiClient.OrisResponse<>(
                        Map.of(String.valueOf(clubMember.id()), clubMember), "JSON", "OK", null, "getClubUserList"));
    }
}
