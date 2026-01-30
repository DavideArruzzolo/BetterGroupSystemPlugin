package dzve.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import dzve.config.BetterGroupSystemPluginConfig;

import java.time.Instant;

public class LogService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new Gson();
    private static boolean debugMode = false;

    public static void updateConfig(BetterGroupSystemPluginConfig config) {
        if (config != null) {
            debugMode = config.isDebugMode();
        }
    }

    public static void info(String context, String message, Object... details) {
        log("INFO", context, message, details);
    }

    public static void warn(String context, String message, Object... details) {
        log("WARN", context, message, details);
    }

    public static void error(String context, String message, Throwable throwable, Object... details) {
        log("ERROR", context, message, details);
        if (throwable != null) {
            LOGGER.atSevere().withCause(throwable).log(formatMessage("ERROR", context, message));
        }
    }

    public static void debug(String context, String message, Object... details) {
        if (debugMode) {
            log("DEBUG", context, message, details);
        }
    }

    private static void log(String level, String context, String message, Object... details) {
        String jsonLog = buildJsonLog(level, context, message, details);

        // Output to Hytale Logger based on level
        if ("ERROR".equals(level)) {
            LOGGER.atSevere().log(jsonLog);
        } else if ("WARN".equals(level)) {
            LOGGER.atWarning().log(jsonLog);
        } else {
            LOGGER.atInfo().log(jsonLog);
        }
    }

    private static String formatMessage(String level, String context, String message) {
        return String.format("[%s] [%s] %s", level, context, message);
    }

    private static String buildJsonLog(String level, String context, String message, Object[] details) {
        JsonObject logObject = new JsonObject();
        logObject.addProperty("timestamp", Instant.now().toString());
        logObject.addProperty("level", level);
        logObject.addProperty("context", context);
        logObject.addProperty("message", message);

        if (details != null && details.length > 0) {
            JsonObject detailsObj = new JsonObject();
            for (int i = 0; i < details.length; i += 2) {
                if (i + 1 < details.length) {
                    detailsObj.addProperty(String.valueOf(details[i]), String.valueOf(details[i + 1]));
                }
            }
            logObject.add("details", detailsObj);
        }

        return GSON.toJson(logObject);
    }
}
