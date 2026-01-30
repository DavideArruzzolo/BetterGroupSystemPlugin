package dzve.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.Source;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.service.group.GroupService;
import dzve.utils.LogService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class PvPProtectionSystem extends DamageEventSystem {
    final Config<dzve.config.BetterGroupSystemPluginConfig> config;

    public PvPProtectionSystem(Config<BetterGroupSystemPluginConfig> config) {
        this.config = config;
    }

    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> buf, @NonNullDecl Damage damage) {
        if (config.get().isPvpEnabled()) {
            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            PlayerRef victim = store.getComponent(victimRef, PlayerRef.getComponentType());
            if (victim != null) {
                Source source = damage.getSource();
                if (source instanceof EntitySource entitySource) {
                    Ref<EntityStore> attackerRef = entitySource.getRef();
                    PlayerRef attacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
                    if (attacker != null) {
                        Group victimGroup = GroupService.getInstance().getGroupOrNotify(victim);
                        Group attackerGroup = GroupService.getInstance().getGroupOrNotify(attacker);

                        if (victimGroup != null && attackerGroup != null) {
                            if (victimGroup.getId().equals(attackerGroup.getId())) {
                                damage.setCancelled(true);
                                LogService.debug("PVP", "Cancelled PvP", "attacker", attacker.getUsername(), "victim",
                                        victim.getUsername(), "reason", "SAME_GROUP");
                                attacker.sendMessage(
                                        Message.raw("You cannot hurt a member of your own group.").color(Color.YELLOW));
                            } else if (victimGroup.getDiplomacyStatus(attackerGroup.getId()) == DiplomacyStatus.ALLY) {
                                damage.setCancelled(true);
                                LogService.debug("PVP", "Cancelled PvP", "attacker", attacker.getUsername(), "victim",
                                        victim.getUsername(), "reason", "ALLY");
                                attacker.sendMessage(
                                        Message.raw("You cannot hurt an allied group member.").color(Color.YELLOW));
                            }
                        }
                    }
                }
            }
        }
    }

    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}