package com.klabis.groups.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Sealed hierarchy of filter objects for querying the three group aggregate types.
 * Each subtype carries only the filter fields that back an actual caller — YAGNI.
 */
@ValueObject
public sealed interface GroupFilter permits FreeGroupFilter, TrainingGroupFilter, FamilyGroupFilter {
}
