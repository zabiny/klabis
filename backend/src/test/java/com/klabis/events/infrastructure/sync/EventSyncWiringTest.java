package com.klabis.events.infrastructure.sync;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncLine;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the {@code oris} profile wires a valid ORIS event {@link SyncLine} into the context and
 * that a PUSH for {@code SyncType.EVENT} never escapes as an exception — ORIS is a read-only sync
 * source, so a push resolves to an ERROR {@link SyncRecord} whose failure cause names the reason.
 */
@SpringBootTest(classes = {TestApplicationConfiguration.class})
@ActiveProfiles({"test", "oris"})
@DisplayName("ORIS event sync wiring")
class EventSyncWiringTest {

    @Autowired
    private DataSync dataSync;

    @Autowired(required = false)
    private SyncLine<EventSyncData, EventSyncData> orisEventSyncLine;

    @Test
    @DisplayName("the ORIS event SyncLine bean is present in the oris-profile context")
    void syncLineBeanPresent() {
        assertThat(orisEventSyncLine).isNotNull();
        assertThat(orisEventSyncLine.localSource().type()).isEqualTo(SyncType.EVENT);
        assertThat(orisEventSyncLine.externalSource().type()).isEqualTo(SyncType.EVENT);
    }

    @Test
    @DisplayName("PUSH for an event never throws and yields an ERROR SyncRecord")
    void pushNeverThrows() {
        // No bootstrap event exists in the events schema for this test, so a local-id PUSH stops at
        // localSource.fetch (empty) before reaching externalSource.save. Either way the invariant we
        // assert holds: DataSyncImpl swallows the failure into an ERROR SyncRecord rather than
        // letting it escape. The read-only ORIS save exception itself is covered by
        // OrisEventSyncSourceTest#save_throws.
        SyncRecord[] record = new SyncRecord[1];

        assertThatCode(() -> record[0] = dataSync.sync(
                SyncId.localId(SyncType.EVENT, java.util.UUID.randomUUID().toString()), DataSync.Direction.PUSH))
                .doesNotThrowAnyException();

        assertThat(record[0].result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(record[0].failureCause()).isNotNull();
    }
}
