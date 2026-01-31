package com.hypixel.hytale.logger;

/**
 * Shadow class to replace HytaleLogger during unit tests.
 * Bypasses the static initialization check for HytaleLogManager.
 */
public class HytaleLogger {

    public static HytaleLogger forEnclosingClass() {
        return new HytaleLogger();
    }

    // Static implementations
    public static void info(String message) {
        System.out.println("[HytaleLogger-Stub] STATIC INFO: " + message);
    }

    public static void warn(String message) {
        System.out.println("[HytaleLogger-Stub] STATIC WARN: " + message);
    }

    public static void error(String message) {
        System.err.println("[HytaleLogger-Stub] STATIC ERROR: " + message);
    }

    public static void error(String message, Throwable t) {
        System.err.println("[HytaleLogger-Stub] STATIC ERROR: " + message);
        t.printStackTrace();
    }

    public static void debug(String message) {
        System.out.println("[HytaleLogger-Stub] STATIC DEBUG: " + message);
    }

    // Instance implementations
    public void info(String message, Object... args) {
        System.out.println("[HytaleLogger-Stub] INFO: " + message);
    }

    public void warn(String message, Object... args) {
        System.out.println("[HytaleLogger-Stub] WARN: " + message);
    }

    public void error(String message, Object... args) {
        System.err.println("[HytaleLogger-Stub] ERROR: " + message);
    }

    public void debug(String message, Object... args) {
        System.out.println("[HytaleLogger-Stub] DEBUG: " + message);
    }
}
