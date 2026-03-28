package com.klabis.events.application;

/**
 * Exception thrown when attempting to import an event from ORIS that has already been imported.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateOrisImportException extends RuntimeException {

    public DuplicateOrisImportException(int orisId) {
        super("Event with ORIS ID " + orisId + " has already been imported");
    }
}
