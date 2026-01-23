package dzve.service.group;

import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.model.Permission;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of GroupDiplomacyService following Java 25 best practices.
 * Uses pattern matching, records, and sealed interfaces for type safety and performance.
 */
public final class GroupDiplomacyServiceImpl implements GroupDiplomacyService {

    private final GroupService groupService;

    public GroupDiplomacyServiceImpl(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public DiplomacyResult setDiplomacy(DiplomacyRequest request) {
        try {
            Group sourceGroup = groupService.getGroupForPlayer(request.playerId());
            if (sourceGroup == null) {
                return new DiplomacyFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_DIPLOMACY)) {
                return new DiplomacyFailure("You don't have permission to manage diplomacy");
            }

            // Find the target group
            Group targetGroup = groupService.findGroupByNameOrTag(request.targetGroupIdentifier());
            if (targetGroup == null) {
                return new DiplomacyFailure("Target group not found");
            }

            // Prevent setting diplomacy with own group
            if (sourceGroup.getId().equals(targetGroup.getId())) {
                return new DiplomacyFailure("Cannot set diplomatic status with your own group");
            }

            // Check if status is already the same
            DiplomacyStatus currentStatus = sourceGroup.getDiplomacyStatus(targetGroup.getId());
            if (currentStatus == request.status()) {
                return new DiplomacyFailure("Diplomatic status is already set to " + request.status());
            }

            // Handle ally requests (require acceptance)
            boolean requiresAcceptance = false;
            if (request.status() == DiplomacyStatus.ALLY) {
                // For ally requests, we set a pending status that requires acceptance
                // For now, we'll implement direct alliance - in a full implementation,
                // you'd want to handle ally requests and acceptance flow
                requiresAcceptance = true;
            }

            // Set the diplomatic status
            boolean success = groupService.setDiplomacy(
                    sourceGroup.getId(),
                    targetGroup.getId(),
                    request.status()
            );

            if (success) {
                return new DiplomacySuccess(
                        sourceGroup.getId(),
                        targetGroup.getId(),
                        request.status(),
                        requiresAcceptance
                );
            } else {
                return new DiplomacyFailure("Failed to set diplomatic status");
            }

        } catch (Exception e) {
            return new DiplomacyFailure("Failed to set diplomacy: " + e.getMessage());
        }
    }

    @Override
    public Optional<Map<UUID, DiplomacyStatus>> getDiplomaticRelations(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        return Optional.of(Map.copyOf(group.getDiplomaticRelations()));
    }

    @Override
    public Optional<List<DiplomaticRelation>> listDiplomacy(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        List<DiplomaticRelation> relations = group.getDiplomaticRelations().entrySet().stream()
                .map(entry -> {
                    UUID groupId = entry.getKey();
                    DiplomacyStatus status = entry.getValue();
                    Group targetGroup = groupService.getGroup(groupId);

                    if (targetGroup != null) {
                        return new DiplomaticRelation(
                                groupId,
                                targetGroup.getName(),
                                targetGroup.getTag(),
                                status
                        );
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return Optional.of(relations);
    }

    @Override
    public DiplomacyStatus getDiplomacyStatus(UUID groupId1, UUID groupId2) {
        Group group1 = groupService.getGroup(groupId1);
        if (group1 == null) {
            return DiplomacyStatus.NEUTRAL;
        }

        return group1.getDiplomacyStatus(groupId2);
    }

    @Override
    public boolean areAllies(UUID groupId1, UUID groupId2) {
        return getDiplomacyStatus(groupId1, groupId2) == DiplomacyStatus.ALLY;
    }

    @Override
    public boolean areEnemies(UUID groupId1, UUID groupId2) {
        return getDiplomacyStatus(groupId1, groupId2) == DiplomacyStatus.ENEMY;
    }
}
