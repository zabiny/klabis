package com.klabis.members.infrastructure.orissync;

/**
 * Thrown by {@link OrisClubMembers} when an operation needing the ORIS club key is
 * attempted while none is held (design.md D9). Says plainly that the club key has
 * not been set rather than failing as though ORIS itself were at fault — ORIS is
 * never contacted when this is thrown.
 */
public class ClubKeyNotSetException extends RuntimeException {

    public ClubKeyNotSetException() {
        super("The ORIS club key has not been set");
    }
}
