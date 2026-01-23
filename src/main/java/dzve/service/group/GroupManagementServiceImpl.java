package dzve.service.group;

import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.enums.GroupType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class GroupManagementServiceImpl implements GroupManagementService {

    private final GroupService coreGroupService;
    private final BetterGroupSystemPluginConfig config;
    private static final Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9]*$");


    public GroupManagementServiceImpl(GroupService coreGroupService, BetterGroupSystemPluginConfig config) {
        this.coreGroupService = coreGroupService;
        this.config = config;
    }

    @Override
    public GroupCreationResult createGroup(GroupCreationRequest request) {
        if (coreGroupService.getGroupForPlayer(request.leaderId()) != null) {
            return new GroupCreationFailure("You are already in a group.");
        }
        if (request.name().length() < config.getMinNameLength() || request.name().length() > config.getMaxNameLength()) {
            return new GroupCreationFailure("Group name must be between " + config.getMinNameLength() + " and " + config.getMaxNameLength() + " characters.");
        }
        if (request.tag().length() < config.getMinTagLength() || request.tag().length() > config.getMaxTagLength()) {
            return new GroupCreationFailure("Group tag must be between " + config.getMinTagLength() + " and " + config.getMaxTagLength() + " characters.");
        }
        if (!ALPHANUMERIC.matcher(request.name()).matches() || !ALPHANUMERIC.matcher(request.tag()).matches()) {
            return new GroupCreationFailure("Group name and tag must be alphanumeric.");
        }
        if (coreGroupService.getGroupByName(request.name()).isPresent() || coreGroupService.getGroupByTag(request.tag()).isPresent()) {
            return new GroupCreationFailure("A group with that name or tag already exists.");
        }

        Set<GroupMember> members = new HashSet<>();
        members.add(new GroupMember(request.leaderId(), null, LocalDateTime.now()));

        Group group = new Group(
                UUID.randomUUID(),
                config.getPluginMode() == BetterGroupSystemPluginConfig.PluginMode.FACTION ? GroupType.FACTION : GroupType.GUILD,
                request.name(),
                request.tag(),
                "",
                "#FFFFFF",
                request.leaderId(),
                1,
                0.0,
                members,
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                LocalDateTime.now()
        );

        coreGroupService.saveGroup(group);
        return new GroupCreationSuccess(group);
    }

    @Override
    public GroupUpdateResult updateGroup(GroupUpdateRequest request) {
        Optional<Group> groupOpt = coreGroupService.getGroupById(request.groupId());
        if (groupOpt.isEmpty()) {
            return new GroupUpdateFailure("Group not found.");
        }
        Group group = groupOpt.get();

        if (request.name() != null) {
            if (request.name().length() < config.getMinNameLength() || request.name().length() > config.getMaxNameLength()) {
                return new GroupUpdateFailure("Group name must be between " + config.getMinNameLength() + " and " + config.getMaxNameLength() + " characters.");
            }
            if (!ALPHANUMERIC.matcher(request.name()).matches()) {
                return new GroupUpdateFailure("Group name must be alphanumeric.");
            }
            if (coreGroupService.getGroupByName(request.name()).isPresent() && !coreGroupService.getGroupByName(request.name()).get().getId().equals(group.getId())) {
                return new GroupUpdateFailure("A group with that name already exists.");
            }
            group.setName(request.name());
        }

        if (request.tag() != null) {
            if (request.tag().length() < config.getMinTagLength() || request.tag().length() > config.getMaxTagLength()) {
                return new GroupUpdateFailure("Group tag must be between " + config.getMinTagLength() + " and " + config.getMaxTagLength() + " characters.");
            }
            if (!ALPHANUMERIC.matcher(request.tag()).matches()) {
                return new GroupUpdateFailure("Group tag must be alphanumeric.");
            }
            if (coreGroupService.getGroupByTag(request.tag()).isPresent() && !coreGroupService.getGroupByTag(request.tag()).get().getId().equals(group.getId())) {
                return new GroupUpdateFailure("A group with that tag already exists.");
            }
            group.setTag(request.tag());
        }

        if (request.description() != null) {
            group.setDescription(request.description());
        }

        coreGroupService.saveGroup(group);
        return new GroupUpdateSuccess(group);
    }

    @Override
    public GroupDeletionResult deleteGroup(GroupDeletionRequest request) {
        Optional<Group> groupOpt = coreGroupService.getGroupById(request.groupId());
        if (groupOpt.isEmpty()) {
            return new GroupDeletionFailure("Group not found.");
        }
        Group group = groupOpt.get();
        if (!group.getLeaderId().equals(request.leaderId())) {
            return new GroupDeletionFailure("You are not the leader of this group.");
        }

        coreGroupService.deleteGroup(group.getId());
        return new GroupDeletionSuccess(group.getId());
    }

    @Override
    public Optional<Group> getGroupInfo(String groupName) {
        return coreGroupService.getGroupByName(groupName);
    }

    @Override
    public GroupLeaveResult leaveGroup(UUID playerId) {
        Group group = coreGroupService.getGroupForPlayer(playerId);
        if (group == null) {
            return new GroupLeaveFailure("You are not in a group.");
        }
        if (group.getLeaderId().equals(playerId)) {
            if (group.getMembers().size() > 1) {
                return new GroupLeaveFailure("You must transfer leadership before leaving the group.");
            } else {
                coreGroupService.deleteGroup(group.getId());
                return new GroupLeaveSuccess(playerId, group.getId());
            }
        }

        group.getMembers().removeIf(member -> member.getPlayerId().equals(playerId));
        coreGroupService.saveGroup(group);
        return new GroupLeaveSuccess(playerId, group.getId());
    }

    @Override
    public Group getGroupForPlayer(UUID playerId) {
        return coreGroupService.getGroupForPlayer(playerId);
    }
}
