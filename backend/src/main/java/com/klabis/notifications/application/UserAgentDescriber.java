package com.klabis.notifications.application;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

final class UserAgentDescriber {

    private static final String UNKNOWN = "Neznámé zařízení";

    // Order matters: Edge and Opera UAs also contain "Chrome", Chrome UAs also contain "Safari".
    private static final Map<String, String> BROWSERS = new LinkedHashMap<>();
    // Order matters: iOS UAs also contain "like Mac OS X", Android UAs also contain "Linux".
    private static final Map<String, String> SYSTEMS = new LinkedHashMap<>();

    static {
        BROWSERS.put("Edg/", "Edge");
        BROWSERS.put("OPR/", "Opera");
        BROWSERS.put("Firefox/", "Firefox");
        BROWSERS.put("FxiOS/", "Firefox");
        BROWSERS.put("CriOS/", "Chrome");
        BROWSERS.put("Chrome/", "Chrome");
        BROWSERS.put("Safari/", "Safari");

        SYSTEMS.put("Android", "Android");
        SYSTEMS.put("iPhone", "iOS");
        SYSTEMS.put("iPad", "iOS");
        SYSTEMS.put("Windows", "Windows");
        SYSTEMS.put("Mac OS X", "macOS");
        SYSTEMS.put("Linux", "Linux");
    }

    private UserAgentDescriber() {
    }

    static String describe(@Nullable String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String browser = lookup(BROWSERS, userAgent);
        String system = lookup(SYSTEMS, userAgent);
        if (browser == null && system == null) {
            return UNKNOWN;
        }
        if (browser == null) {
            return system;
        }
        return system == null ? browser : browser + " · " + system;
    }

    private static @Nullable String lookup(Map<String, String> table, String userAgent) {
        return table.entrySet().stream()
                .filter(e -> userAgent.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
