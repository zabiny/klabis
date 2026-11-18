package com.klabis.members.application;

public final class MemberOptionPrompt {

    private MemberOptionPrompt() {
    }

    public static String of(String firstName, String lastName, String registrationNumber) {
        return "%s %s (%s)".formatted(firstName, lastName, registrationNumber);
    }
}
