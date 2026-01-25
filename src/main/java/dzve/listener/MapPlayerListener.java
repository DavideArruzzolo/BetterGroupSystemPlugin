package dzve.listener;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.BetterGroupSystemPlugin;
import dzve.model.Group;
import dzve.service.group.GroupService;

import java.util.UUID;
import java.util.logging.Level;

public class MapPlayerListener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final BetterGroupSystemPlugin plugin;

    public MapPlayerListener(BetterGroupSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        try {
            Player player = event.getPlayer();
            WorldMapTracker mapTracker = player.getWorldMapTracker();

            UUID playerId = player.getUuid();
            GroupService service = GroupService.getInstance(null);
            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef != null) {
                this.plugin.getTracker().registerPlayer(playerRef);
            }

            this.updateMapFilter(mapTracker, playerId, service);
        } catch (Exception var7) {
            LOGGER.at(Level.WARNING).log("Error setting up player map filter: %s", var7.getMessage());
        }

    }

    public void updatePlayerMapFilter(Player player) {
        try {
            WorldMapTracker mapTracker = player.getWorldMapTracker();

            UUID playerId = player.getUuid();
            GroupService service = GroupService.getInstance(null);
            this.updateMapFilter(mapTracker, playerId, service);
        } catch (Exception var5) {
            this.plugin.getLogger().at(Level.WARNING).log("Error updating player map filter: %s", var5.getMessage());
        }

    }

    private void updateMapFilter(WorldMapTracker mapTracker, UUID playerId, GroupService service) {
        mapTracker.setPlayerMapFilter(otherPlayer -> {
            UUID otherPlayerId = otherPlayer.getUuid();
            if (otherPlayerId.equals(playerId)) {
                return false;
            } else {
                Group currentGroup = service.getPlayerGroup(playerId);
                if (currentGroup == null) {
                    return true;
                } else {
                    boolean isMember = currentGroup.isMember(otherPlayerId);
                    return !isMember;
                }
            }
        });
    }
}