package dzve.systems;


import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class DamageEvent extends DamageEventSystem {

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl Damage damage) {
        if (damage.getSource() instanceof Damage.EntitySource entity) {

            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            Ref<EntityStore> attacker_ref = entity.getRef();

            Player player = store.getComponent(ref, Player.getComponentType());
            NPCEntity attacker = store.getComponent(attacker_ref, NPCEntity.getComponentType());

            if (attacker != null) {
                if (player != null) {
                    player.sendMessage(Message.raw("You're getting attacked by " + attacker.getRoleName() + "!"));
                }
            }

            damage.setCancelled(true);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}