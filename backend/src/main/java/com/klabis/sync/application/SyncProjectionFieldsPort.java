package com.klabis.sync.application;

import com.klabis.sync.domain.SyncProjection;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Map;

/**
 * Breaks a {@link SyncProjection} into its named fields for the per-field divergence
 * shape of the sync state response.
 */
@PrimaryPort
public interface SyncProjectionFieldsPort {

    Map<String, Object> fields(SyncProjection projection);
}
