package com.klabis.common.authorization;

/**
 * Kind of object an authority is about. {@link #NONE} marks authorities that concern no particular target.
 */
public enum TargetType {
    MEMBER,
    EVENT,
    NONE
}
