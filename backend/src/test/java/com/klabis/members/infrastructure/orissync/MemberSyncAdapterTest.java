package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSyncAdapter")
class MemberSyncAdapterTest {

    @Mock
    private ManagementPort managementPort;

    @Mock
    private RegistrationPort registrationPort;

    @Mock
    private OrisClubMembers orisClubMembers;

    private MemberSyncAdapter adapter;

    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final MemberId MEMBER_ID = new MemberId(MEMBER_UUID);
    private static final String ORIS_ID = "33630";

    @BeforeEach
    void setUp() {
        adapter = new MemberSyncAdapter(managementPort, registrationPort, orisClubMembers);
    }

    @Test
    @DisplayName("declares pull-only-creating capabilities, containing sensitive data: no outward write, creates the local side")
    void declaresPullOnlyCreatingCapabilitiesWithSensitiveData() {
        var capabilities = adapter.capabilities();

        assertThat(capabilities.readsLocal()).isTrue();
        assertThat(capabilities.readsExternal()).isTrue();
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isTrue();
        assertThat(capabilities.createsExternal()).isFalse();
        assertThat(capabilities.containsSensitiveData()).isTrue();
    }

    @Nested
    @DisplayName("applyToExternal()")
    class ApplyToExternalMethod {

        @Test
        @DisplayName("throws — the adapter declares no outward write capability")
        void throwsUnsupported() {
            MemberProjection projection = referenceProjection();

            assertThatThrownBy(() -> adapter.applyToExternal(ORIS_ID, projection))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("createLocal()")
    class CreateLocalMethod {

        @Test
        @DisplayName("imports a member via RegistrationPort using the ORIS registration number and returns the new id")
        void importsMemberAndReturnsId() {
            MemberProjection projection = referenceProjection();
            Member imported = MemberTestDataBuilder.aMemberWithId(MEMBER_UUID).build();
            when(registrationPort.importMember(any(RegistrationPort.ImportMember.class))).thenReturn(imported);

            String entityId = adapter.createLocal(projection);

            ArgumentCaptor<RegistrationPort.ImportMember> captor =
                    ArgumentCaptor.forClass(RegistrationPort.ImportMember.class);
            Mockito.verify(registrationPort).importMember(captor.capture());

            RegistrationPort.ImportMember command = captor.getValue();
            assertThat(command.registrationNumber()).isEqualTo(new RegistrationNumber(projection.registrationNumber()));
            assertThat(command.details().personalInformation().getFirstName()).isEqualTo(projection.firstName());
            assertThat(command.details().personalInformation().getLastName()).isEqualTo(projection.lastName());
            assertThat(command.details().email().value()).isEqualTo(projection.email());
            assertThat(command.details().phone().value()).isEqualTo(projection.phone());
            assertThat(command.details().address().street()).isEqualTo(projection.street());
            assertThat(command.details().bankAccountNumber()).isNull();
            assertThat(command.details().registeredBy()).isNull();

            assertThat(entityId).isEqualTo(MEMBER_ID.value().toString());
        }

        @Test
        @DisplayName("imports a member whose ORIS record lacks address, e-mail, phone and birth number")
        void importsMemberWithoutOptionalContactData() {
            MemberProjection sparse = sparseProjection();
            when(registrationPort.importMember(any(RegistrationPort.ImportMember.class)))
                    .thenReturn(MemberTestDataBuilder.aMemberWithId(MEMBER_UUID).build());

            adapter.createLocal(sparse);

            ArgumentCaptor<RegistrationPort.ImportMember> captor =
                    ArgumentCaptor.forClass(RegistrationPort.ImportMember.class);
            Mockito.verify(registrationPort).importMember(captor.capture());
            RegistrationPort.RegisterNewMember details = captor.getValue().details();
            assertThat(details.address()).isNull();
            assertThat(details.email()).isNull();
            assertThat(details.phone()).isNull();
            assertThat(details.birthNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("applyToLocal()")
    class ApplyToLocalMethod {

        @Test
        @DisplayName("builds a Member.SyncFromOris from the projection and calls ManagementPort.syncMemberFromOris")
        void buildsSyncFromOrisAndCallsManagementPort() {
            MemberProjection projection = referenceProjection();
            Member updated = MemberTestDataBuilder.aMemberWithId(MEMBER_UUID).build();
            when(managementPort.syncMemberFromOris(any(MemberId.class), any(Member.SyncFromOris.class)))
                    .thenReturn(updated);

            adapter.applyToLocal(MEMBER_UUID.toString(), projection);

            ArgumentCaptor<Member.SyncFromOris> captor = ArgumentCaptor.forClass(Member.SyncFromOris.class);
            Mockito.verify(managementPort).syncMemberFromOris(eq(MEMBER_ID), captor.capture());

            Member.SyncFromOris command = captor.getValue();
            assertThat(command.registrationNumber()).isEqualTo(new RegistrationNumber(projection.registrationNumber()));
            assertThat(command.firstName()).isEqualTo(projection.firstName());
            assertThat(command.lastName()).isEqualTo(projection.lastName());
            assertThat(command.email().value()).isEqualTo(projection.email());
            assertThat(command.phone().value()).isEqualTo(projection.phone());
            assertThat(command.address().street()).isEqualTo(projection.street());
            assertThat(command.chipNumber()).isEqualTo(projection.chipNumber());
        }

        @Test
        @DisplayName("passes absent address, e-mail, phone, birth number and chip number through as null, letting ORIS clear them")
        void passesAbsentOptionalDataThroughAsNull() {
            MemberProjection sparse = sparseProjection();
            when(managementPort.syncMemberFromOris(any(MemberId.class), any(Member.SyncFromOris.class)))
                    .thenReturn(MemberTestDataBuilder.aMemberWithId(MEMBER_UUID).build());

            adapter.applyToLocal(MEMBER_UUID.toString(), sparse);

            ArgumentCaptor<Member.SyncFromOris> captor = ArgumentCaptor.forClass(Member.SyncFromOris.class);
            Mockito.verify(managementPort).syncMemberFromOris(eq(MEMBER_ID), captor.capture());
            Member.SyncFromOris command = captor.getValue();
            assertThat(command.address()).isNull();
            assertThat(command.email()).isNull();
            assertThat(command.phone()).isNull();
            assertThat(command.birthNumber()).isNull();
            assertThat(command.chipNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("readLocal()")
    class ReadLocalMethod {

        @Test
        @DisplayName("maps the local Member read through ManagementPort into the canonical projection")
        void mapsMemberIntoProjection() {
            Member realMember = MemberTestDataBuilder.aMember().build();
            when(managementPort.getMember(any(MemberId.class))).thenReturn(realMember);

            SyncProjection projection = adapter.readLocal(realMember.getId().value().toString());

            assertThat(projection).isInstanceOf(MemberProjection.class);
            assertThat(((MemberProjection) projection).firstName()).isEqualTo(realMember.getFirstName());
            assertThat(((MemberProjection) projection).registrationNumber())
                    .isEqualTo(realMember.getRegistrationNumber().getValue());
        }
    }

    @Nested
    @DisplayName("readExternal()")
    class ReadExternalMethod {

        @Test
        @DisplayName("maps the matching ORIS club member entry into the canonical projection")
        void mapsMatchingEntryIntoProjection() {
            ClubMember clubMember = orisClubMember(ORIS_ID, "ZBM0001", "Jan", "Novák");
            stubClubMembers(clubMember);

            SyncProjection projection = adapter.readExternal(ORIS_ID);

            assertThat(projection).isInstanceOf(MemberProjection.class);
            assertThat(((MemberProjection) projection).firstName()).isEqualTo("Jan");
            assertThat(((MemberProjection) projection).lastName()).isEqualTo("Novák");
            assertThat(((MemberProjection) projection).registrationNumber()).isEqualTo("ZBM0001");
        }

        @Test
        @DisplayName("fails meaningfully when no ORIS club member matches the external id")
        void throwsWhenNoEntryMatches() {
            stubClubMembers(orisClubMember("99999", "ZBM0002", "Petr", "Svoboda"));

            assertThatThrownBy(() -> adapter.readExternal(ORIS_ID))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    private static MemberProjection sparseProjection() {
        MemberProjection full = referenceProjection();
        return new MemberProjection(
                full.registrationNumber(), full.firstName(), full.lastName(), full.dateOfBirth(),
                full.gender(), full.nationality(), null, null, null, null, null, null, null, null);
    }

    private static MemberProjection referenceProjection() {
        return new MemberProjection(
                "ZBM0001",
                "Jan",
                "Novák",
                LocalDate.of(1990, 1, 15),
                Gender.MALE,
                "CZ",
                "9001151234",
                "jan@example.com",
                "+420123456789",
                "Testovací 1",
                "Brno",
                "60000",
                "CZ",
                "12345"
        );
    }

    private static ClubMember orisClubMember(String id, String regNum, String firstName, String lastName) {
        final int parsedId;
        try {
            parsedId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid club member id: " + id, e);
        }

        return ClubMemberBuilder.builder()
                .id(parsedId)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user")
                .firstName(firstName)
                .lastName(lastName)
                .email("jan@example.com")
                .street("Testovací 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .birthday(LocalDate.of(1990, 1, 15))
                .phone("+420123456789")
                .gender("M")
                .persNum("9001151234")
                .nationality("CZ")
                .si("12345")
                .build();
    }

    private void stubClubMembers(ClubMember... clubMembers) {
        Map<String, ClubMember> byId = new java.util.LinkedHashMap<>();
        for (ClubMember clubMember : clubMembers) {
            byId.put(String.valueOf(clubMember.id()), clubMember);
        }
        when(orisClubMembers.listClubMembers()).thenReturn(byId);
    }
}
