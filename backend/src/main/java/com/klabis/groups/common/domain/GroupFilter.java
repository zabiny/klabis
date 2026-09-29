package com.klabis.groups.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Hierarchy of filter objects for querying the three group aggregate types.
 * Each subtype carries only the filter fields that back an actual caller — YAGNI.
 */
@ValueObject
public interface GroupFilter {
}
