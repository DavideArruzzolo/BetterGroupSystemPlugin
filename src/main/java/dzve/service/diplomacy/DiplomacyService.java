package dzve.service.diplomacy;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.Permission;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;
import dzve.utils.LogService;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Warning;

public class DiplomacyService {

    private final GroupService groupService;

    // Map of target group UUID -> Set of requesting group UUIDs
    private final Map<UUID, Set<UUID>> allyRequests = new ConcurrentHashMap<>();

    public DiplomacyService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void setDiplomacy(PlayerRef sender, String targetGroupName, DiplomacyStatus status) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        LogService.debug("DIPLOMACY", "Setting diplomacy", "sender", sender.getUsername(), "target", targetGroupName,
                "status", status);

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
            // Send ally request instead of setting directly
            sendAllyRequest(sender, group, target);
        } else {
            // For NEUTRAL or ENEMY, check if currently allied and remove both sides
            if (group.getDiplomacyStatus(target.getId()) == DiplomacyStatus.ALLY) {
                // Breaking alliance - remove from both sides
                group.setDiplomacyStatus(target.getId(), status);
                target.setDiplomacyStatus(group.getId(), DiplomacyStatus.NEUTRAL);

                // Notify target group
                NotificationService.getInstance().broadcastGroup(
                        target.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                        group.getName() + " has broken the alliance with your group.",
                        Warning);

                // Persist the target's side of the broken alliance
                groupService.persistSetDiplomacy(target.getId(), group.getId(), DiplomacyStatus.NEUTRAL);
            } else {
                group.setDiplomacyStatus(target.getId(), status);
            }

            groupService.persistSetDiplomacy(group.getId(), target.getId(), status);
            // Bidirectional?
            // "The other group sees us as NEUTRAL?" -> No, diplomacy table is bidirectional
            // logically if we insert (g1, g2, status).
            // My DAO sets (g1, g2, status). It doesn't autoset (g2, g1, status) unless I
            // call it.
            // But previous logic was: group.setDiplomacy(target, status). Target wasn't
            // updated in memory?
            // "target.setDiplomacyStatus(group.getId(), DiplomacyStatus.NEUTRAL);" was
            // called above for breaking alliance.
            // But if just setting ENEMY, it only updated 'group'.

            // If I want to persist strictly what happened in memory:
            // persistSetDiplomacy(group.getId(), target.getId(), status);

            groupService.notify(sender, "Diplomacy with " + target.getName() + " set to " + status, false);
        }
    }

    /**
     * Send an alliance request to another group.
     */
    private void sendAllyRequest(PlayerRef sender, Group from, Group to) {
        LogService.debug("DIPLOMACY", "Sending ally request", "from", from.getName(), "to", to.getName());
        // Check if already allied
        if (from.getDiplomacyStatus(to.getId()) == DiplomacyStatus.ALLY) {
            groupService.notify(sender, "You are already allied with " + to.getName() + ".");
            return;
        }

        // Check if request already pending
        Set<UUID> pendingRequests = allyRequests.get(to.getId());
        if (pendingRequests != null && pendingRequests.contains(from.getId())) {
            groupService.notify(sender, "Alliance request already pending.");
            return;
        }

        // Check if the target has already sent us a request (auto-accept)
        Set<UUID> ourPendingRequests = allyRequests.get(from.getId());
        if (ourPendingRequests != null && ourPendingRequests.contains(to.getId())) {
            // They already requested us, so accept automatically
            establishAlliance(from, to);
            ourPendingRequests.remove(to.getId());
            groupService.notify(sender, "Alliance established with " + to.getName() + "! (They had already requested)",
                    false);
            return;
        }

        // Add request
        allyRequests.computeIfAbsent(to.getId(), k -> ConcurrentHashMap.newKeySet()).add(from.getId());

        groupService.notify(sender, "Alliance request sent to " + to.getName() + ".", false);

        // Notify target group members
        NotificationService.getInstance().broadcastGroup(
                to.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                from.getName() + " has requested an alliance! Use '/faction acceptally " + from.getName()
                        + "' to accept.",
                Success);
    }

    /**
     * Accept an alliance request from another group.
     */
    public void acceptAllyRequest(PlayerRef sender, String targetGroupName) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (!groupService.hasPerm(group, sender, Permission.CAN_MANAGE_DIPLOMACY)) {
            return;
        }

        Group requestingGroup = groupService.getGroupByName(targetGroupName);
        if (requestingGroup == null) {
            groupService.notify(sender, "Group not found.");
            return;
        }

        Set<UUID> pendingRequests = allyRequests.get(group.getId());
        if (pendingRequests == null || !pendingRequests.contains(requestingGroup.getId())) {
            groupService.notify(sender, "No alliance request from " + targetGroupName + ".");
            return;
        }

        // Establish alliance
        establishAlliance(group, requestingGroup);
        pendingRequests.remove(requestingGroup.getId());

        groupService.notify(sender, "Alliance established with " + requestingGroup.getName() + "!", false);

        // Notify the requesting group
        NotificationService.getInstance().broadcastGroup(
                requestingGroup.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                group.getName() + " has accepted your alliance request!",
                Success);
    }

    /**
     * Deny an alliance request from another group.
     */
    public void denyAllyRequest(PlayerRef sender, String targetGroupName) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (!groupService.hasPerm(group, sender, Permission.CAN_MANAGE_DIPLOMACY)) {
            return;
        }

        Group requestingGroup = groupService.getGroupByName(targetGroupName);
        if (requestingGroup == null) {
            groupService.notify(sender, "Group not found.");
            return;
        }

        Set<UUID> pendingRequests = allyRequests.get(group.getId());
        if (pendingRequests == null || !pendingRequests.contains(requestingGroup.getId())) {
            groupService.notify(sender, "No alliance request from " + targetGroupName + ".");
            return;
        }

        pendingRequests.remove(requestingGroup.getId());
        groupService.notify(sender, "Alliance request from " + requestingGroup.getName() + " denied.", false);

        // Notify the requesting group
        NotificationService.getInstance().broadcastGroup(
                requestingGroup.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                group.getName() + " has denied your alliance request.",
                Warning);
    }

    /**
     * List pending alliance requests for the player's group.
     */
    public void listAllyRequests(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        Set<UUID> pendingRequests = allyRequests.get(group.getId());

        ChatFormatter.StyledText msg = ChatFormatter.of("=== Pending Alliance Requests ===\n\n")
                .withColor(Color.YELLOW).withBold();

        if (pendingRequests == null || pendingRequests.isEmpty()) {
            msg = msg.append("No pending alliance requests.").withColor(Color.GRAY);
        } else {
            for (UUID requestingGroupId : pendingRequests) {
                Group requestingGroup = groupService.getGroup(requestingGroupId);
                if (requestingGroup != null) {
                    msg = msg.append("  ● ").withColor(Color.GREEN)
                            .append(requestingGroup.getName()).withColor(Color.GREEN)
                            .append(" (" + requestingGroup.getTag() + ")\n").withColor(Color.GRAY);
                }
            }
            msg = msg.append("\nUse '/faction acceptally <name>' or '/faction denyally <name>'").withColor(Color.GRAY);
        }

        sender.sendMessage(msg.toMessage());
    }

    /**
     * Establish a bidirectional alliance between two groups.
     */
    private void establishAlliance(Group group1, Group group2) {
        LogService.info("DIPLOMACY", "Establishing alliance", "group1", group1.getName(), "group2", group2.getName());
        group1.setDiplomacyStatus(group2.getId(), DiplomacyStatus.ALLY);
        group2.setDiplomacyStatus(group1.getId(), DiplomacyStatus.ALLY);

        groupService.persistSetDiplomacy(group1.getId(), group2.getId(), DiplomacyStatus.ALLY);
        groupService.persistSetDiplomacy(group2.getId(), group1.getId(), DiplomacyStatus.ALLY);
    }

    public void listDiplomacy(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        final ChatFormatter.StyledText[] msg = {
                ChatFormatter.of("=== Diplomatic Relations for " + group.getName() + " ===\n\n")
                        .withColor(Color.YELLOW).withBold() };

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
