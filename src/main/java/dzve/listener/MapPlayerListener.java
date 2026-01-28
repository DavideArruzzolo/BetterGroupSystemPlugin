package dzve.listener;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.BetterGroupSystemPlugin;
import dzve.service.group.GroupService;
import dzve.utils.MapUtils;

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

            MapUtils.updateMapFilter(mapTracker, playerUuid, service);
        } catch (Exception var7) {
            LOGGER.at(Level.WARNING).log("Error setting up player map filter: %s", var7.getMessage());
        }

    }
}