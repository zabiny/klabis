package com.klabis.members.infrastructure.orissync;

import com.klabis.members.domain.Gender;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberProjection")
class MemberProjectionTest {

    @Test
    @DisplayName("implements SyncProjection and reports SyncEntityType.MEMBER")
    void declaresMemberEntityType() {
        MemberProjection projection = new MemberProjection(
                "ZBM0001", "Jan", "Novák",
                LocalDate.of(1990, 1, 15), Gender.MALE, "CZ",
                "900115/0000", "jan@example.com", "+420700000001",
                "Testovací 1", "Brno", "600 00", "CZ",
                "1000001"
        );

        assertThat(projection).isInstanceOf(SyncProjection.class);
        assertThat(projection.entityType()).isEqualTo(SyncEntityType.MEMBER);
    }

    @Test
    @DisplayName("carries exactly the ORIS-owned fields declared in design.md D2 — no Klabis-owned field is expressible")
    void carriesOnlyOrisOwnedFields() {
        List<String> expectedComponents = List.of(
                "registrationNumber", "firstName", "lastName",
                "dateOfBirth", "gender", "nationality",
                "birthNumber", "email", "phone",
                "street", "city", "postalCode", "country",
                "chipNumber"
        );

        RecordComponent[] components = MemberProjection.class.getRecordComponents();
        Set<String> actualComponents = Arrays.stream(components)
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(actualComponents).containsExactlyInAnyOrderElementsOf(expectedComponents);

        List<String> klabisOwnedFieldNames = List.of(
                "identityCard", "drivingLicenseGroup", "medicalCourse", "trainerLicense",
                "refereeLicense", "dietaryRestrictions", "guardian", "bankAccountNumber",
                "suspensionReason", "suspendedAt", "suspensionNote", "suspendedBy", "active"
        );
        assertThat(actualComponents).doesNotContainAnyElementsOf(klabisOwnedFieldNames);
    }
}
