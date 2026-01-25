package dzve.service;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import dzve.config.BetterGroupSystemPluginConfig;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class NotificationService {
    private static final String DEFAULT_ICON = "Weapon_Sword_Nexus";
    private static final NotificationService istance = new NotificationService();

    public static NotificationService getInstance() {
        return istance;
    }

    public void broadcastGroup(List<UUID> players, String message, NotificationStyle style) {
        for (UUID player : players) {
            sendNotification(player, message, style);
        }
    }

    public void sendNotification(UUID playerUuid, String message, NotificationStyle style) {
        Objects.requireNonNull(playerUuid, "Player UUID cannot be null");
        Objects.requireNonNull(message, "Secondary message cannot be null");

        String prefix = buildNotificationPrefix(style);
        sendCustomNotification(playerUuid, message, prefix, DEFAULT_ICON, style);
    }

    public void sendCustomNotification(UUID playerUuid, String primaryMessage, String secondaryMessage,
                                       String iconItem, NotificationStyle notificationStyle) {
        Objects.requireNonNull(playerUuid, "Player UUID cannot be null");
        Objects.requireNonNull(primaryMessage, "Primary message cannot be null");
        Objects.requireNonNull(iconItem, "Icon item cannot be null");

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            return;
        }

        PacketHandler packetHandler = playerRef.getPacketHandler();
        Message primaryMsg = Message.raw(primaryMessage);
        Message secondaryMsg = Message.raw(secondaryMessage);
        ItemWithAllMetadata icon = new ItemStack(iconItem).toPacket();
        NotificationStyle style = notificationStyle != null ? notificationStyle : NotificationStyle.Default;

        NotificationUtil.sendNotification(packetHandler, primaryMsg, secondaryMsg, icon, style);
    }

    private String buildNotificationPrefix(NotificationStyle style) {
        return BetterGroupSystemPluginConfig.MOD_NAME + switch (style) {
            case Danger -> " -> ERROR";
            case Success -> " -> SUCCESS";
            case Warning -> " -> WARNING";
            case Default -> "";
            case null -> "";
        };
    }
}
