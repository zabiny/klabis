package com.klabis.sync;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.application.ConflictNotAcknowledgedException;
import com.klabis.sync.application.SyncRecordClaimedException;
import com.klabis.sync.application.SyncRecordNeedsResolutionException;
import com.klabis.sync.application.SyncRecordNotFailedException;
import com.klabis.sync.application.SyncRecordNotFoundException;
import com.klabis.sync.application.SyncRecordNotInConflictException;
import com.klabis.sync.application.UnsupportedResolutionException;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sync error types")
class SyncErrorTypesTest {

    private static final SyncRecordId ID = new SyncRecordId(UUID.randomUUID());

    @Test
    @DisplayName("a record that needs a decision reports its status")
    void needsResolution() {
        SyncRecordNeedsResolutionException ex = new SyncRecordNeedsResolutionException(ID, SyncStatus.CONFLICT);

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.errorType()).isEqualTo("sync-record-needs-resolution");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("syncStatus", "CONFLICT"));
    }

    @Test
    @DisplayName("an unsupported resolution reports the resolution")
    void unsupportedResolution() {
        UnsupportedResolutionException ex = new UnsupportedResolutionException(ID, SyncResolution.OUTWARD);

        assertThat(ex).isInstanceOf(ConflictException.class);
        assertThat(ex.errorType()).isEqualTo("unsupported-resolution");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("resolution", "OUTWARD"));
    }

    @Test
    @DisplayName("the remaining state conflicts are 409 errors of their own types without parameters")
    void stateConflicts() {
        Map<String, UserFacingError> expected = Map.of(
                "sync-record-not-in-conflict", new SyncRecordNotInConflictException(ID),
                "conflict-not-acknowledged", new ConflictNotAcknowledgedException(ID),
                "sync-record-not-failed", new SyncRecordNotFailedException(ID),
                "sync-record-claimed", new SyncRecordClaimedException(ID));

        expected.forEach((type, error) -> {
            assertThat(error).isInstanceOf(ConflictException.class);
            assertThat(error.errorType()).isEqualTo(type);
            assertThat(error.messageParameters()).isEmpty();
        });
    }

    @Test
    @DisplayName("an unknown record is a 404 without parameters")
    void recordNotFound() {
        SyncRecordNotFoundException ex = new SyncRecordNotFoundException(ID);

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.errorType()).isEqualTo("sync-record-not-found");
        assertThat(ex.messageParameters()).isEmpty();
    }
}
