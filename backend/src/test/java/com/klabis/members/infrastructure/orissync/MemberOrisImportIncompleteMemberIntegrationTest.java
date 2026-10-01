package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberCompleteness;
import com.klabis.members.domain.GuardianContacts;
import com.klabis.members.domain.MissingDataItem;
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
 * Mirrors {@link MemberOrisSyncScenarioIntegrationTest}'s discovery scenario for an ORIS member
 * with incomplete data (tasks.md 5.3, design.md D5): a minor with neither a phone nor a birth
 * number is still brought into Klabis, paired for synchronisation, and reported incomplete with
 * exactly the missing items ORIS itself cannot supply.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@MockitoBean(types = {OrisApiClient.class, OrisWebUrls.class})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Members on the sync engine — importing an incomplete ORIS member")
class MemberOrisImportIncompleteMemberIntegrationTest {

    @Autowired
    private OrisClubKeyPort orisClubKeyPort;

    @Autowired
    private MemberDiscoveryJob memberDiscoveryJob;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private ManagementPort managementPort;

    @Autowired
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(744_000);

    private int orisId;
    private String regNum;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        regNum = "ZBM%04d".formatted(orisId % 10000);
        orisClubKeyPort.store("test-club-key");
    }

    @Test
    @DisplayName("a minor without a phone or birth number is imported, paired, and reported missing GUARDIAN, PHONE and BIRTH_NUMBER")
    void minorWithoutPhoneOrBirthNumberIsImportedAndReportedIncomplete() {
        stubClubMembers(incompleteMinorClubMember(orisId, regNum));

        memberDiscoveryJob.discoverNewMembers();

        SyncRecord record = enrolledRecord();
        assertThat(record.getExternalReference())
                .isEqualTo(new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        Member imported = managementPort.getMember(memberIdOf(record));
        assertThat(imported.getRegistrationNumber().getValue()).isEqualTo(regNum);
        assertThat(imported.getPhone()).isNull();
        assertThat(imported.getBirthNumber()).isNull();
        assertThat(MemberCompleteness.missingData(imported, GuardianContacts.NONE)).containsExactlyInAnyOrder(
                MissingDataItem.GUARDIAN, MissingDataItem.PHONE, MissingDataItem.BIRTH_NUMBER);
        assertThat(imported.isDataIncomplete()).isTrue();
    }

    private SyncRecord enrolledRecord() {
        SyncedEntityReference reference = synchronizationPort.findByExternalReferences(
                        SyncEntityType.MEMBER, ExternalSystem.ORIS, List.of(String.valueOf(orisId)))
                .stream()
                .findFirst()
                .orElseThrow();
        return synchronizationPort.findByTarget(reference.target()).orElseThrow();
    }

    @Test
    @DisplayName("an adult with an incomplete ORIS address is imported without an address and reported missing ADDRESS")
    void memberWithIncompleteAddressIsImportedWithoutAddress() {
        stubClubMembers(adultWithIncompleteAddressClubMember(orisId, regNum));

        memberDiscoveryJob.discoverNewMembers();

        SyncRecord record = enrolledRecord();
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        Member imported = managementPort.getMember(memberIdOf(record));
        assertThat(imported.getAddress()).isNull();
        assertThat(MemberCompleteness.missingData(imported, GuardianContacts.NONE)).containsExactly(MissingDataItem.ADDRESS);
        assertThat(imported.isDataIncomplete()).isTrue();
    }

    private static MemberId memberIdOf(SyncRecord record) {
        return new MemberId(UUID.fromString(record.getTarget().entityId()));
    }

    /**
     * A current member (under 18) with an e-mail but neither a phone nor a birth number - ORIS
     * never carries a guardian, so this member is missing exactly GUARDIAN, PHONE and
     * BIRTH_NUMBER, and nothing else.
     */
    private static ClubMember incompleteMinorClubMember(int id, String regNum) {
        return ClubMemberBuilder.builder()
                .id(id)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user" + id)
                .firstName("Anna")
                .lastName("Malá")
                .email(regNum.toLowerCase() + "@example.com")
                .street("Dětská 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .birthday(LocalDate.now().minusYears(10))
                .phone(null)
                .gender("F")
                .persNum(null)
                .nationality("CZ")
                .si("0")
                .build();
    }

    /**
     * A complete adult with an e-mail, phone and birth number - only the postal code is missing
     * from ORIS's address, so the whole address is dropped (design.md ADDRESS: all-or-nothing)
     * and nothing else is missing.
     */
    private static ClubMember adultWithIncompleteAddressClubMember(int id, String regNum) {
        return ClubMemberBuilder.builder()
                .id(id)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user" + id)
                .firstName("Karel")
                .lastName("Úplný")
                .email(regNum.toLowerCase() + "@example.com")
                .street("Úplná 1")
                .city("Ostrava")
                .zip(null)
                .country("CZ")
                .birthday(LocalDate.of(1985, 3, 20))
                .phone("+420601000001")
                .gender("M")
                .persNum("850320/1234")
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
