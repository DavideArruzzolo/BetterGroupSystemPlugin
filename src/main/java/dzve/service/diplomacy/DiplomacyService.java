package dzve.service.diplomacy;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.Permission;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DiplomacyService {

    private final GroupService groupService;

    public DiplomacyService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void setDiplomacy(PlayerRef sender, String targetGroupName, DiplomacyStatus status) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (!groupService.hasPerm(group, sender, Permission.CAN_MANAGE_DIPLOMACY)) {

            return;
        }

        Group target = groupService.getGroupByName(targetGroupName);

        if (target == null) {
            groupService.notify(sender, "Target group not found.");
            return;
        }
        if (target.getId().equals(group.getId())) {
            groupService.notify(sender, "Cannot change relations with yourself.");
            return;
        }

        if (status == DiplomacyStatus.ALLY) {
            groupService.notify(sender, "Alliance request sent (Not implemented fully).", false);

        } else {
            group.setDiplomacyStatus(target.getId(), status);
            groupService.saveGroups();
            groupService.notify(sender, "Diplomacy with " + target.getName() + " set to " + status, false);
        }
    }

    public void listDiplomacy(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        final ChatFormatter.StyledText[] msg = {
                ChatFormatter.of("=== Diplomatic Relations for " + group.getName() + " ===\n\n")
                        .withColor(Color.YELLOW).withBold()};

        if (group.getDiplomaticRelations().isEmpty()) {
            msg[0] = msg[0].append("No diplomatic relations set.").withColor(Color.GRAY);
        } else {

            Map<DiplomacyStatus, List<Group>> relationsByStatus = new HashMap<>();

            group.getDiplomaticRelations().forEach((groupId, status) -> {
                Group relatedGroup = groupService.getGroup(groupId);
                if (relatedGroup != null) {
                    relationsByStatus.computeIfAbsent(status, k -> new ArrayList<>()).add(relatedGroup);
                }
            });

            if (relationsByStatus.containsKey(DiplomacyStatus.ALLY)) {
                msg[0] = msg[0].append("Allies:\n").withColor(Color.GREEN).withBold();
                for (Group ally : relationsByStatus.get(DiplomacyStatus.ALLY)) {
                    msg[0] = msg[0].append("  ● ").withColor(Color.GREEN)
                            .append(ally.getName()).withColor(Color.GREEN)
                            .append(" (" + ally.getTag() + ")\n").withColor(Color.GRAY);
                }
                msg[0] = msg[0].append("\n");
            }

            if (relationsByStatus.containsKey(DiplomacyStatus.ENEMY)) {
                msg[0] = msg[0].append("Enemies:\n").withColor(Color.RED).withBold();
                for (Group enemy : relationsByStatus.get(DiplomacyStatus.ENEMY)) {
                    msg[0] = msg[0].append("  ● ").withColor(Color.RED)
                            .append(enemy.getName()).withColor(Color.RED)
                            .append(" (" + enemy.getTag() + ")\n").withColor(Color.GRAY);
                }
                msg[0] = msg[0].append("\n");
            }

            if (relationsByStatus.containsKey(DiplomacyStatus.NEUTRAL)) {
                msg[0] = msg[0].append("Neutral:\n").withColor(Color.GRAY).withBold();
                for (Group neutral : relationsByStatus.get(DiplomacyStatus.NEUTRAL)) {
                    msg[0] = msg[0].append("  ● ").withColor(Color.GRAY)
                            .append(neutral.getName()).withColor(Color.GRAY)
                            .append(" (" + neutral.getTag() + ")\n").withColor(Color.GRAY);
                }
            }
        }
        sender.sendMessage(msg[0].toMessage());
    }

    public void sendAllyMessage(PlayerRef sender, String[] message) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        String[] messageContent = message.length > 1 ? Arrays.copyOfRange(message, 1, message.length) : new String[0];

        ChatFormatter.StyledText styledMessage = ChatFormatter.of("[AllyChat]")
                .withBold()
                .withMonospace()
                .withColor(Color.GREEN)
                .append("[")
                .withColor(Color.GRAY)
                .append(group.getTag())
                .withColor(Color.decode(group.getColor()))
                .append("] ")
                .withColor(Color.GRAY)
                .append(sender.getUsername() + ": ")
                .append(String.join(" ", messageContent));

        Set<UUID> allRecipients = new HashSet<>();

        group.getMembers().stream()
                .map(GroupMember::getPlayerId)
                .forEach(allRecipients::add);

        group.getDiplomaticRelations().entrySet().stream()
                .filter(entry -> entry.getValue() == DiplomacyStatus.ALLY)
                .map(entry -> groupService.getGroup(entry.getKey()))
                .filter(Objects::nonNull)
                .flatMap(allyGroup -> allyGroup.getMembers().stream())
                .map(GroupMember::getPlayerId)
                .forEach(allRecipients::add);

        Universe.get().getPlayers().stream()
                .filter(player -> allRecipients.contains(player.getUuid()))
                .forEach(player -> player.sendMessage(styledMessage.toMessage()));
    }
}
