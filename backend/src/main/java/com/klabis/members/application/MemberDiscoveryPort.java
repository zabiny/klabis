package com.klabis.members.application;

/**
 * Lets a primary adapter trigger an ORIS member discovery pass (design.md D11) without
 * depending on {@code MemberDiscoveryJob} directly, which is classified
 * {@code @Application}. The only implementation is profile-gated to {@code oris}
 * ({@code @OrisIntegrationComponent}), so callers hold it as {@code Optional}.
 */
@org.jmolecules.architecture.hexagonal.PrimaryPort
public interface MemberDiscoveryPort {

    void discoverNewMembers();
}
