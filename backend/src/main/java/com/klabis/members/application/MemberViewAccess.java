package com.klabis.members.application;

/**
 * What the viewer of a member detail may see, decided by the caller from the authorization rules; the service
 * only acts on it.
 *
 * @param suspendedVisible    a suspended member is visible to the viewer
 * @param birthNumberVisible  the viewer is shown the member's birth number, so viewing it is audited
 */
public record MemberViewAccess(boolean suspendedVisible, boolean birthNumberVisible) {
}
