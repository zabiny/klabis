package com.klabis.common.sync;

/**
 * Interface for data that can be synchronized. It shall provide later support methods to calculate checksum for data so we are able to determine if data has changed or not. Maybe change it to wrapper instead of interface (so actual data object which most likely will belong into domain will not require to implement this interface)
 */
public interface SyncData {
}
