package dzve.listener;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.model.Group;
import dzve.service.group.GroupService;

import java.awt.*;
import java.util.UUID;

public class ChatListener {
    public static void onPlayerChat(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        UUID playerId = sender.getUuid();
        GroupService manager = GroupService.getInstance(null);
        Group group = manager.getPlayerGroup(playerId);
        if (group != null) {
            event.setFormatter((playerRef, message) ->
                    Message.join(Message.raw("[").color(Color.GRAY), Message.raw(group.getTag()).color(group.getColor()), Message.raw("] ").color(Color.GRAY), Message.raw(sender.getUsername()).color(Color.WHITE), Message.raw(": " + message).color(Color.WHITE)));
        }

    }
}
