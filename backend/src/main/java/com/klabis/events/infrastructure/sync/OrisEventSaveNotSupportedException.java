package com.klabis.events.infrastructure.sync;

/**
 * Raised when a sync attempts to write an event back to ORIS. ORIS is a read-only sync source for
 * Klabis — the ORIS client has no write API — so PUSH for {@code SyncType.EVENT} can never succeed.
 * <p>
 * A named type (rather than {@link UnsupportedOperationException}) is what {@code SyncRecord}
 * carries in its {@code failureException} and what the ORIS import service rethrows directly.
 */
class OrisEventSaveNotSupportedException extends RuntimeException {

    OrisEventSaveNotSupportedException(int orisId) {
        super("ORIS is a read-only sync source; cannot push event %d to ORIS".formatted(orisId));
    }
}
