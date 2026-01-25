package dzve.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DamageTrackerSystem extends DamageEventSystem {

    // Track last attacker for each player - shared with PowerDeathSystem
    public static final Map<UUID, UUID> lastAttackerMap = new ConcurrentHashMap<>();

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> buf, Damage damage) {
        // Only track player-to-player damage
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        Player victim = store.getComponent(victimRef, Player.getComponentType());
        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());

        if (victim == null || victimPlayerRef == null) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        Player attacker = store.getComponent(attackerRef, Player.getComponentType());
        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());

        if (attacker == null || attackerPlayerRef == null) {
            return;
        }

        // Don't track self-damage
        if (victimPlayerRef.getUuid().equals(attackerPlayerRef.getUuid())) {
            return;
        }

        // Record the attacker for the victim
        lastAttackerMap.put(victimPlayerRef.getUuid(), attackerPlayerRef.getUuid());
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
