package dzve.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import dzve.utils.LocalDateTimeAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonStorage<T> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Gson gson;
    private final File file;
    private final Class<T> type;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public JsonStorage(File file, Class<T> type) {
        this.file = file;
        this.type = type;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        createFileIfNotExists();
    }

    private void createFileIfNotExists() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.atSevere().log("Failed to create file: " + file.getAbsolutePath(), e);
            }
        }
    }

    public void saveAsync(T data) {
        // Note: Ensure 'data' is not modified by other threads while this runs, 
        // or pass a deep copy/snapshot of the data here.
        executor.submit(() -> saveSync(data));
    }

    public void saveSync(T data) {
        lock.writeLock().lock();
        try {
            File tempFile = new File(file.getAbsolutePath() + ".tmp");
            File backupFile = new File(file.getAbsolutePath() + ".bak");

            // 1. Write to temp file with full flush to disk
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 Writer writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {

                gson.toJson(data, writer);
                writer.flush();
                bos.flush();
                // Force the OS to write the buffer to the physical disk
                fos.getChannel().force(true);
            }

            // 2. Create backup of current file if it exists
            if (file.exists()) {
                try {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.atWarning().log("Failed to create backup file: " + e.getMessage());
                    // We continue even if backup fails, as saving the new data is priority,
                    // but ideally we want the backup.
                }
            }

            // 3. Atomic move temp to actual file
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to save data to " + file.getAbsolutePath(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T load() {
        lock.readLock().lock();
        try {
            if (!file.exists()) {
                return null;
            }
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                return gson.fromJson(reader, type);
            } catch (Exception e) {
                LOGGER.atSevere().log("Failed to load data from " + file.getAbsolutePath() + ". Detected corruption or error. Attempting recovery...", e);
                return attemptRecovery();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private T attemptRecovery() {
        File backupFile = new File(file.getAbsolutePath() + ".bak");
        if (backupFile.exists()) {
            LOGGER.atInfo().log("Found backup file. Attempting to recover from: " + backupFile.getAbsolutePath());
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(backupFile), StandardCharsets.UTF_8))) {
                T data = gson.fromJson(reader, type);
                LOGGER.atInfo().log("Recovery successful! Restoring main file from backup...");

                // Restore the main file immediately to prevent future issues
                // We need a write lock for this, but we currently hold a read lock.
                // Upgrading locks is not possible in ReentrantReadWriteLock, so we must release and acquire.
                // However, since we are inside 'load' which returns data, we can just return the data 
                // and let the next save fix the file, OR we can try to fix it now.
                // Fixing it now is safer.

                restoreMainFileFromBackup(backupFile);

                return data;
            } catch (Exception e) {
                LOGGER.atSevere().log("Failed to recover from backup. Data may be lost.", e);
            }
        } else {
            LOGGER.atSevere().log("No backup file found. Recovery impossible.");
        }
        return null;
    }

    private void restoreMainFileFromBackup(File backupFile) {
        // We need to release the read lock before acquiring the write lock to avoid deadlock
        // But 'load' holds the read lock. 
        // Since this is a private method called by load, we are in a tricky spot regarding locks if we want to be 100% strict.
        // However, simply copying the file doesn't strictly require the write lock on the *Java object* 
        // if we assume no one else is writing to the file system at this exact moment (which the write lock guards).
        // To be safe, we should just copy the file bytes.

        try {
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to restore main file from backup on disk.", e);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
