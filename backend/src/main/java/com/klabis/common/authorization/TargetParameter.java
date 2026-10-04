package com.klabis.common.authorization;

/**
 * Position and kind of the method parameter that identifies the target of an invocation.
 */
public record TargetParameter(int index, TargetType type) {
}
