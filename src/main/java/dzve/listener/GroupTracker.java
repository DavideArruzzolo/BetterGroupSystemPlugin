package dzve.listener;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GroupTracker {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Map<UUID, PlayerRef> playerCache;

    public GroupTracker() {
        this.playerCache = new ConcurrentHashMap<>();
    }

    public void registerPlayer(PlayerRef playerRef) {
        this.playerCache.put(playerRef.getUuid(), playerRef);
    }

    public void unregisterPlayer(UUID playerId) {
        this.playerCache.remove(playerId);
    }

    public PlayerRef getPlayer(UUID playerId) {
        return this.playerCache.get(playerId);
    }

    public Collection<PlayerRef> getAllPlayers() {
        return this.playerCache.values();
    }

    public void start() {
        LOGGER.at(Level.INFO).log("Group tracker initialized (map filtering not available in current API)");
    }

    public void stop() {
    }
}