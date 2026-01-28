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
        GroupService service = GroupService.getInstance(null);

        MapUtils.updateMapFilter(mapTracker, playerUuid, service);
    }
}