package com.klabis.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;

/**
 * Custom Logback converter that colors log levels with custom colors.
 *
 * <p>Colors:
 * <ul>
 *   <li>ERROR: Red</li>
 *   <li>WARN: Yellow</li>
 *   <li>INFO: Blue</li>
 *   <li>DEBUG: Green (custom, not default white)</li>
 *   <li>TRACE: Default</li>
 * </ul>
 */
public class HighlightLevelConverter extends ClassicConverter {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String MAGENTA = "\u001B[35m";

    @Override
    public String convert(ILoggingEvent event) {
        Level level = event.getLevel();
        String levelStr = event.getLevel().toString();

        switch (level.levelInt) {
            case Level.ERROR_INT:
                return RED + levelStr + RESET;
            case Level.WARN_INT:
                return YELLOW + levelStr + RESET;
            case Level.INFO_INT:
                return BLUE + levelStr + RESET;
            case Level.DEBUG_INT:
                return GREEN + levelStr + RESET;
            case Level.TRACE_INT:
                return MAGENTA + levelStr + RESET;
            default:
                return levelStr;
        }
    }
}
