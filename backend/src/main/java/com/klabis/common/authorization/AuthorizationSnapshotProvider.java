package com.klabis.common.authorization;

/**
 * Supplies the permission snapshot of the current request. Every call within one request returns the same
 * instance, so all decisions of the request see one set of permissions.
 */
public interface AuthorizationSnapshotProvider {

    AuthorizationSnapshot current();
}
