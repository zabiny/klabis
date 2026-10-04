package com.klabis.common.authorization;

/**
 * Ways an authority may be held: over everything of its target type (including targets created later)
 * or only over specific targets derived from relationships.
 */
public enum GrantForm {
    ALL,
    SPECIFIC
}
