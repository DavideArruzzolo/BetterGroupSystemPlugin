package dzve.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonStorage<T> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final ObjectMapper objectMapper;
    private final File file;
    private final Class<T> type;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public JsonStorage(File file, Class<T> type) {
        this.file = file;
        this.type = type;
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        createFileIfNotExists();
    }

    private void createFileIfNotExists() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    if (!file.getParentFile().mkdirs()) {
                        LOGGER.atWarning().log("Could not create parent directories for: {}", file.getAbsolutePath());
                    }
                }
                if (!file.createNewFile()) {
                    LOGGER.atWarning().log("Could not create file: {}", file.getAbsolutePath());
                }
            } catch (IOException e) {
                LOGGER.atSevere().withCause(e).log("Failed to create file: {}", file.getAbsolutePath());
            }
        }
    }

    public void save(T data) {
        lock.writeLock().lock();
        try {
            File tempFile = new File(file.getAbsolutePath() + ".tmp");
            File backupFile = new File(file.getAbsolutePath() + ".bak");

            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                objectMapper.writeValue(bos, data);
            }

            if (file.exists()) {
                try {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.atWarning().log("Failed to create backup file: {}", e.getMessage());
                }
            }

            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.atSevere().withCause(e).log("Failed to move temp file to final destination");
            }

        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save data to {}", file.getAbsolutePath());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T load() {
        lock.readLock().lock();
        try {
            if (!file.exists() || file.length() == 0) {
                return null;
            }
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                return objectMapper.readValue(isr, type);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log(
                        "Failed to load data from {}. Detected corruption or error. Attempting recovery...",
                        file.getAbsolutePath());
                return attemptRecovery();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private T attemptRecovery() {
        File backupFile = new File(file.getAbsolutePath() + ".bak");
        if (backupFile.exists()) {
            LOGGER.atInfo().log("Found backup file. Attempting to recover from: {}", backupFile.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(backupFile);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                T data = objectMapper.readValue(isr, type);
                LOGGER.atInfo().log("Recovery successful! Restoring main file from backup...");
                restoreMainFileFromBackup(backupFile);
                return data;
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to recover from backup. Data may be lost.");
            }
        } else {
            LOGGER.atSevere().log("No backup file found. Recovery impossible.");
        }
        return null;
    }

    private void restoreMainFileFromBackup(File backupFile) {
        try {
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to restore main file from backup on disk.");
        }
    }
}
