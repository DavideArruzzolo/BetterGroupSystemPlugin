package dzve.utils;

import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.service.group.GroupService;

import java.util.UUID;

public class MapUtils {

    public static void updateMapFilter(WorldMapTracker mapTracker, UUID playerId, GroupService service) {
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
                return false;
            }

            Group otherGroup = service.getPlayerGroup(otherPlayerId);
            if (otherGroup != null) {
                DiplomacyStatus status = currentGroup.getDiplomacyStatus(otherGroup.getId());
                if (status == DiplomacyStatus.ALLY) {
                    return false;
                }
            }

            return true;
        });
    }

    public static void clearMapFilter(WorldMapTracker mapTracker, UUID playerId) {
        mapTracker.setPlayerMapFilter(otherPlayer -> {

            return false;
        });
    }
}
