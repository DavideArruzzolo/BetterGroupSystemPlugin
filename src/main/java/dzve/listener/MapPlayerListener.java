package dzve.listener;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.BetterGroupSystemPlugin;
import dzve.model.DiplomacyStatus;
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
            UUIDComponent uuidComponent = null;
            if (event.getPlayer().getReference() != null) {
                uuidComponent = event.getPlayer().getReference().getStore().getComponent(event.getPlayerRef(), UUIDComponent.getComponentType());
            }
            if (uuidComponent == null)
                return;
            UUID playerUuid = uuidComponent.getUuid();
            GroupService service = GroupService.getInstance(null);

            this.updateMapFilter(mapTracker, playerUuid, service);
        } catch (Exception var7) {
            LOGGER.at(Level.WARNING).log("Error setting up player map filter: %s", var7.getMessage());
        }

    }

    @SuppressWarnings("deprecation")
    private void updateMapFilter(WorldMapTracker mapTracker, UUID playerId, GroupService service) {
        mapTracker.setPlayerMapFilter(otherPlayer -> {
            UUID otherPlayerId = otherPlayer.getUuid();
            if (otherPlayerId.equals(playerId)) {
                return false;
            }

            Group currentGroup = service.getPlayerGroup(playerId);
            if (currentGroup == null) {
                return true;
            }

            if (currentGroup.isMember(otherPlayerId)) {
                // TODO: Imposta il colore per i membri del gruppo (se l'API lo supporta)
                return false;
            }

            Group otherGroup = service.getPlayerGroup(otherPlayerId);
            if (otherGroup != null) {
                DiplomacyStatus status = currentGroup.getDiplomacyStatus(otherGroup.getId());
                // TODO: Imposta il colore per gli alleati (se l'API lo supporta)
                return status != DiplomacyStatus.ALLY;
            }

            return true;
        });
    }
}