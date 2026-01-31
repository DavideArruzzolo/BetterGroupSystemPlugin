package dzve.listener;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.service.group.GroupService;
import dzve.utils.MapUtils;

import java.util.UUID;

public class MapPlayerListener {

    public MapPlayerListener() {
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        WorldMapTracker mapTracker = player.getWorldMapTracker();
        UUIDComponent uuidComponent = null;
        if (event.getPlayer().getReference() != null) {
            uuidComponent = event.getPlayer().getReference().getStore().getComponent(event.getPlayerRef(),
                    UUIDComponent.getComponentType());
        }
        if (uuidComponent == null)
            return;
        UUID playerUuid = uuidComponent.getUuid();
        GroupService service = GroupService.getInstance();

        if (GroupService.getConfig().isHidePlayers()) {
            MapUtils.updateMapFilter(mapTracker, playerUuid, service);
        } else {
            MapUtils.clearMapFilter(mapTracker, playerUuid);
        }

        com.hypixel.hytale.server.core.universe.PlayerRef playerRef = player.getReference().getStore().getComponent(
                player.getReference(), com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        if (playerRef != null) {
            dzve.utils.LogService.debug("LISTENER", "Updated map filter for player " + playerRef.getUsername()
                    + " (HidePlayers: " + GroupService.getConfig().isHidePlayers() + ")");
        }
    }
}