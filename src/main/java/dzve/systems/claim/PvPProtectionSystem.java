package dzve.systems.claim;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.Source;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.service.group.GroupService;

import java.awt.*;
import java.util.UUID;

public class PvPProtectionSystem extends DamageEventSystem {
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buf, Damage damage) {
        if (!BetterGroupSystemPluginConfig.getInstance().isPvpEnabled()) {
            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            PlayerRef victim = store.getComponent(victimRef, PlayerRef.getComponentType());
            if (victim != null) {
                Source source = damage.getSource();
                if (source instanceof EntitySource entitySource) {
                    Ref<EntityStore> attackerRef = entitySource.getRef();
                    if (attackerRef != null) {
                        PlayerRef attacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
                        if (attacker != null) {
                            UUID victimId = victim.getUuid();
                            UUID attackerId = attacker.getUuid();
                            Group victimGroup = GroupService.getInstance(null).getGroupOrNotify(victim);
                            Group attackerGroup = GroupService.getInstance(null).getGroupOrNotify(attacker);

                            if (victimGroup != null && attackerGroup != null) {
                                if (victimGroup.getId().equals(attackerGroup.getId())) {
                                    damage.setCancelled(true);
                                    attacker.sendMessage(Message.raw("You cannot hurt a member of your own group.").color(Color.YELLOW));
                                } else if (victimGroup.getDiplomacyStatus(attackerGroup.getId()) == DiplomacyStatus.ALLY) {
                                    damage.setCancelled(true);
                                    attacker.sendMessage(Message.raw("You cannot hurt an allied group member.").color(Color.YELLOW));
                                }
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