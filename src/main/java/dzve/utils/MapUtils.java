package dzve.utils;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.service.group.GroupService;

import java.util.UUID;

/**
 * Utilità per la gestione della mappa dei giocatori
 */
public class MapUtils {

    @SuppressWarnings("deprecation")
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
