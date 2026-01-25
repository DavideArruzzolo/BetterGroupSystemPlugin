package dzve.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Faction;
import dzve.model.Group;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Default;

public class PowerDeathSystem extends DamageEventSystem {

    private static final NotificationService notificationService = NotificationService.getInstance();

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> buf, Damage damage) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) {
            return;
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID victimId = playerRef.getUuid();
        Group victimGroup = GroupService.getInstance(null).getPlayerGroup(victimId);

        if (victimGroup == null || !(victimGroup instanceof Faction victimFaction)) {
            return;
        }

        BetterGroupSystemPluginConfig config = GroupService.getConfig();
        double powerLoss = config.getPowerLooseByDeath();

        victimFaction.removePlayerPower(victimId, powerLoss);
        victimFaction.incrementDeaths();

        notificationService.sendNotification(victimId,
                "You lost " + powerLoss + " power from death!", Default);

        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            Player attacker = store.getComponent(attackerRef, Player.getComponentType());
            PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());

            if (attacker != null && attackerPlayerRef != null) {
                UUID killerId = attackerPlayerRef.getUuid();
                Group killerGroup = GroupService.getInstance(null).getPlayerGroup(killerId);

                if (killerGroup != null && killerGroup instanceof Faction killerFaction) {
                    if (!killerGroup.equals(victimGroup)) {
                        double powerGain = config.getPowerGainByKill();

                        killerFaction.addPlayerPower(killerId, powerGain);
                        killerFaction.incrementKills();

                        notificationService.sendNotification(killerId,
                                "You gained " + powerGain + " power from killing " + playerRef.getUsername() + "!", Default);
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
