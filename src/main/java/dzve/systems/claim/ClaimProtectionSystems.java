package dzve.systems.claim;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.model.Faction;
import dzve.model.Group;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import dzve.utils.LogService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Danger;

public class ClaimProtectionSystems {

    private static final NotificationService notificationService = NotificationService.getInstance();
    private static final long RAID_NOTIFICATION_COOLDOWN = TimeUnit.MINUTES.toMillis(5);

    private static boolean isProtected(Store<EntityStore> store, Ref<EntityStore> ref, Vector3i pos) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) {
            return false;
        }

        World world = store.getExternalData().getWorld();
        String worldName = world.getName();
        int chunkX = pos.x >> 5;
        int chunkZ = pos.z >> 5;

        Group group = GroupService.getInstance().getGroupByChunk(worldName, chunkX, chunkZ);
        if (group == null) {
            return false;
        }

        UUID playerId = player.getUuid();
        if (group.isMember(playerId)) {
            return false;
        }

        if (group instanceof Faction faction) {
            if (faction.isRaidable()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - faction.getLastRaidNotificationTimestamp() > RAID_NOTIFICATION_COOLDOWN) {
                    faction.setLastRaidNotificationTimestamp(currentTime);
                    String raidMessage = "Your territory at " + worldName + " (" + chunkX + ", " + chunkZ
                            + ") is being raided by " + player.getUsername() + "!";
                    faction.getMembers().forEach(
                            member -> notificationService.sendNotification(member.getPlayerId(), raidMessage, Danger));
                }
                return false;
            }
        }

        notificationService.sendNotification(player.getUuid(), "This territory is protected by " + group.getName(),
                Danger);
        LogService.debug("CLAIM", "Blocked interaction", "player", player.getUsername(), "group", group.getName(),
                "pos", pos.toString());
        return true;
    }

    public static class UseBlockProtectionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
        public UseBlockProtectionSystem() {
            super(UseBlockEvent.Pre.class);
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> buf, UseBlockEvent.Pre event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            Vector3i pos = event.getTargetBlock();
            if (isProtected(store, ref, pos)) {
                event.setCancelled(true);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }

    public static class BreakBlockProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public BreakBlockProtectionSystem() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> buf, BreakBlockEvent event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            Vector3i pos = event.getTargetBlock();
            if (isProtected(store, ref, pos)) {
                event.setCancelled(true);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }

    public static class PlaceBlockProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        public PlaceBlockProtectionSystem() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> buf, PlaceBlockEvent event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            Vector3i pos = event.getTargetBlock();
            if (isProtected(store, ref, pos)) {
                event.setCancelled(true);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }

    public static class DamageBlockProtectionSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        public DamageBlockProtectionSystem() {
            super(DamageBlockEvent.class);
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> buf, DamageBlockEvent event) {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            Vector3i pos = event.getTargetBlock();
            if (isProtected(store, ref, pos)) {
                event.setCancelled(true);
            }
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }
}