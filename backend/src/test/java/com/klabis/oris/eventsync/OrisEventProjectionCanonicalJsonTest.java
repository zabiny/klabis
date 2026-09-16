package com.klabis.oris.eventsync;

import com.klabis.events.EventTypeId;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression pin for {@link OrisEventProjection}'s canonical JSON form (design.md,
 * first Risk under "Risks / Trade-offs"). This test must NOT be edited to match new
 * output produced by a future change — if it fails, the projection's serialised shape
 * has drifted and that is the change being caught, not a fixture to update.
 */
class OrisEventProjectionCanonicalJsonTest {

    @Test
    @DisplayName("pins the canonical JSON of a fully-populated projection")
    void toCanonicalJson_ofFullyPopulatedProjection_matchesPinnedLiteral() {
        OrisEventProjection projection = new OrisEventProjection(
                "Spring Sprint",
                LocalDate.of(2026, 5, 1),
                "Brno Park",
                "OOB",
                "https://oris.ceskyorientak.cz/Zavod?id=1234",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 20),
                List.of(
                        new OrisEventProjection.Category("101", "M21"),
                        new OrisEventProjection.Category("102", "M20")
                ),
                1,
                "A",
                "Mistrovství republiky",
                new BigDecimal("150.00"),
                "CZK",
                new EventTypeId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        );

        String json = SyncProjectionCodec.toCanonicalJson(projection);

        assertThat(json).isEqualTo("""
                {"baseEntryFeeAmount":150,"baseEntryFeeCurrency":"CZK","categories":[{"name":"M21","orisId":"101"},{"name":"M20","orisId":"102"}],"eventDate":"2026-05-01","location":"Brno Park","name":"Spring Sprint","organizer":"OOB","rankingLevelId":1,"rankingName":"Mistrovství republiky","rankingShortName":"A","registrationDeadline1":"2026-04-01","registrationDeadline2":"2026-04-15","registrationDeadline3":"2026-04-20","websiteUrl":"https://oris.ceskyorientak.cz/Zavod?id=1234"}""");
        assertThat(json).doesNotContain("resolvedEventTypeId");
    }
}
