package com.klabis.sync.application;

import com.klabis.sync.domain.SyncAttempt;
import com.klabis.sync.domain.SyncTarget;

/**
 * An attempt together with what it belongs to — the attempt alone does not know which entity it
 * is about.
 */
public record AttemptView(SyncAttempt attempt, SyncTarget target, String externalId) {
}
