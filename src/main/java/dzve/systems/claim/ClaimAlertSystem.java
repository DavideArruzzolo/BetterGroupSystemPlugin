package dzve.systems.claim;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Group;
import dzve.service.group.GroupService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimAlertSystem extends EntityTickingSystem<EntityStore> {
    private final Map<UUID, String> playerLastTitle = new ConcurrentHashMap<>();

    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> buf) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (playerRef != null && player != null) {
            String wildernessText = "Wilderness";
            Message titleMessage = Message.raw(wildernessText).color(Color.GREEN);
            String titleText = wildernessText;

            int blockX = (int) playerRef.getTransform().getPosition().getX();
            int blockZ = (int) playerRef.getTransform().getPosition().getZ();
            int chunkX = blockX >> 5;
            int chunkZ = blockZ >> 5;
            String worldName = player.getWorld() != null ? player.getWorld().getName() : null;

            Group group = GroupService.getInstance().getGroupByChunk(worldName, chunkX, chunkZ);
            if (group != null) {
                titleText = group.getName();
                titleMessage = Message.raw(titleText).color(new Color(255, 215, 0));
            }

            String previousTitle = playerLastTitle.get(playerRef.getUuid());
            if (!titleText.equals(previousTitle)) {
                playerLastTitle.put(playerRef.getUuid(), titleText);
                EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage,
                        Message.raw(BetterGroupSystemPluginConfig.MOD_NAME), false, null, 2.0f, 0.5f, 0.5f);
            }
        }
    }
}