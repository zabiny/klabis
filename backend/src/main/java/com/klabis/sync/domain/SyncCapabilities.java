package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * What an integration can do for one entity type (design.md D3): which operations
 * exist at all. The engine resolves direction only among the operations that are
 * declared here — a pull-only integration is a first-class, declared fact rather than
 * a method that throws.
 */
@ValueObject
public record SyncCapabilities(
        boolean readsLocal,
        boolean readsExternal,
        boolean writesLocal,
        boolean writesExternal,
        boolean createsLocal,
        boolean createsExternal,
        boolean containsSensitiveData
) {
}
