package dzve.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Faction;
import dzve.model.Group;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Default;

public class PowerDeathSystem extends DeathSystems.OnDeathSystem {

    private static final NotificationService notificationService = NotificationService.getInstance();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if (player == null) {
            return;
        }

        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID victimId = playerRef.getUuid();
        Group victimGroup = GroupService.getInstance(null).getPlayerGroup(victimId);

        if (!(victimGroup instanceof Faction victimFaction)) {
            return;
        }

        BetterGroupSystemPluginConfig config = GroupService.getConfig();
        double powerLoss = config.getPowerLooseByDeath();

        victimFaction.removePlayerPower(victimId, powerLoss);
        victimFaction.incrementDeaths();

        // Save the updated power data to JSON
        GroupService.getInstance(null).saveGroups();

        notificationService.sendNotification(victimId,
                "You lost " + powerLoss + " power from death!", Default);

        // Check if we have a recorded last attacker
        UUID killerId = DamageTrackerSystem.lastAttackerMap.remove(victimId); // Remove after use
        if (killerId != null) {
            Group killerGroup = GroupService.getInstance(null).getPlayerGroup(killerId);

            if (killerGroup instanceof Faction killerFaction) {
                if (!killerGroup.equals(victimGroup)) {
                    double powerGain = config.getPowerGainByKill();

                    killerFaction.addPlayerPower(killerId, powerGain);
                    killerFaction.incrementKills();

                    // Save the updated power data to JSON
                    GroupService.getInstance(null).saveGroups();

                    notificationService.sendNotification(killerId,
                            "You gained " + powerGain + " power from killing " + playerRef.getUsername() + "!", Default);
                }
            }
        }
    }
}
