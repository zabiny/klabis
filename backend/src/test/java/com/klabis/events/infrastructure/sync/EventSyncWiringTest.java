package com.klabis.events.infrastructure.sync;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.sync.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@code oris} profile wires a valid ORIS event {@link SyncLine} into the context (with
 * {@link EventSyncDataConverter} as the reverse converter) and that a PUSH for {@code SyncType.EVENT}
 * never escapes as an exception — ORIS is a read-only sync source, so a push resolves to an ERROR
 * {@link SyncRecord}. The forward converter itself throws {@link UnsupportedOperationException}.
 */
@SpringBootTest(classes = {TestApplicationConfiguration.class})
@ActiveProfiles({"test", "oris"})
@DisplayName("ORIS event sync wiring")
class EventSyncWiringTest {

    @Autowired
    private DataSync dataSync;

    @Autowired(required = false)
    private SyncLine<EventSyncData, OrisEventSyncData> orisEventSyncLine;

    @Test
    @DisplayName("the ORIS event SyncLine bean is present in the oris-profile context")
    void syncLineBeanPresent() {
        assertThat(orisEventSyncLine).isNotNull();
        assertThat(orisEventSyncLine.localSource().type()).isEqualTo(SyncType.EVENT);
        assertThat(orisEventSyncLine.externalSource().type()).isEqualTo(SyncType.EVENT);
        assertThat(orisEventSyncLine.reverseConverter()).isInstanceOf(EventSyncDataConverter.class);
    }

    @Test
    @DisplayName("the forward (PUSH) converter throws UnsupportedOperationException — ORIS is read-only")
    void forwardConverterUnsupported() {
        assertThatThrownBy(() -> orisEventSyncLine.converter().convert(
                new EventSyncData(null, 0, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    @DisplayName("PUSH for an event never throws and yields an ERROR SyncRecord")
    void pushNeverThrows() {
        // No local event with this id exists, so a local-id PUSH stops at localSource.fetch (empty)
        // before reaching the forward converter / externalSource.save. Either way the invariant we
        // assert holds: DataSyncImpl swallows the failure into an ERROR SyncRecord rather than
        // letting it escape. The read-only ORIS save exception is covered by
        // OrisEventSyncSourceTest#save_throws and the converter above.
        SyncRecord[] record = new SyncRecord[1];

        assertThatCode(() -> record[0] = dataSync.sync(
                SyncItemId.localId(SyncType.EVENT, java.util.UUID.randomUUID().toString()), DataSync.Direction.PUSH))
                .doesNotThrowAnyException();

        assertThat(record[0].result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(record[0].failureException()).isNotNull();
    }
}
