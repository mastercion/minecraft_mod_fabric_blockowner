package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtil {
    public enum LogLevel {
        NONE,
        MINIMAL,
        ALL
    }

    private static LogLevel currentLogLevel = LogLevel.ALL;
    private static final Logger LOGGER = LoggerFactory.getLogger("blockowner");

    public static void log(String message, LogLevel level) {
        if (currentLogLevel.ordinal() >= level.ordinal()) {
            LOGGER.info(message);
        }
    }

    public static void setLogLevel(LogLevel logLevel) {
        currentLogLevel = logLevel;
    }

    public static LogLevel getLogLevel() {
        return currentLogLevel;
    }
}
