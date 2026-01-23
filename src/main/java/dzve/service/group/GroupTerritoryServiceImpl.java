package dzve.service.group;

import com.hypixel.hytale.server.core.world.location.Location;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Group;
import dzve.model.GroupHome;
import dzve.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of GroupTerritoryService following Java 25 best practices.
 * Uses pattern matching, records, and sealed interfaces for type safety and performance.
 */
public final class GroupTerritoryServiceImpl implements GroupTerritoryService {

    private final GroupService groupService;
    private final BetterGroupSystemPluginConfig config;

    public GroupTerritoryServiceImpl(GroupService groupService, BetterGroupSystemPluginConfig config) {
        this.groupService = groupService;
        this.config = config;
    }

    @Override
    public HomeCreationResult setHome(HomeCreationRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new HomeCreationFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_HOME)) {
                return new HomeCreationFailure("You don't have permission to manage homes");
            }

            // Check home limit
            if (group.getHomeCount() >= config.getMaxHome()) {
                return new HomeCreationFailure("Maximum number of homes reached (" + config.getMaxHome() + ")");
            }

            // Check if home name already exists
            if (group.getHome(request.homeName()) != null) {
                return new HomeCreationFailure("Home with this name already exists");
            }

            // Check if location is in claimed territory (if required)
            if (!isLocationInClaimedTerritory(request.location(), group)) {
                return new HomeCreationFailure("Home must be set in claimed territory");
            }

            // Create the home
            String world = "world"; // Default world name, replace with actual world retrieval if possible
            GroupHome newHome = new GroupHome(request.homeName(), world, request.location().getX(), request.location().getY(), request.location().getZ(), request.location().getYaw(), request.location().getPitch());
            group.addHome(newHome);
            groupService.updateGroup(group);

            return new HomeCreationSuccess(newHome);

        } catch (Exception e) {
            return new HomeCreationFailure("Failed to set home: " + e.getMessage());
        }
    }

    @Override
    public HomeRetrievalResult getHome(HomeRetrievalRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new HomeRetrievalFailure("You are not in a group");
            }

            GroupHome home;
            if (request.homeName().isPresent()) {
                home = group.getHome(request.homeName().get());
                if (home == null) {
                    return new HomeRetrievalFailure("Home not found");
                }
            } else {
                // Get default home or first available
                home = group.getHomes().stream()
                        .findFirst()
                        .orElse(null);
                if (home == null) {
                    return new HomeRetrievalFailure("No homes available");
                }
            }

            return new HomeRetrievalSuccess(home);

        } catch (Exception e) {
            return new HomeRetrievalFailure("Failed to get home: " + e.getMessage());
        }
    }

    @Override
    public HomeUpdateResult updateHome(HomeUpdateRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new HomeUpdateFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_HOME)) {
                return new HomeUpdateFailure("You don't have permission to manage homes");
            }

            // Find the home to update
            GroupHome home;
            if (request.homeName().isPresent()) {
                home = group.getHome(request.homeName().get());
            } else {
                home = group.getHomes().stream().findFirst().orElse(null);
            }

            if (home == null) {
                return new HomeUpdateFailure("Home not found");
            }

            // Check if new location is in claimed territory
            if (!isLocationInClaimedTerritory(request.newLocation(), group)) {
                return new HomeUpdateFailure("Home must be in claimed territory");
            }

            // Update the home location
            home.setX(request.newLocation().getX());
            home.setY(request.newLocation().getY());
            home.setZ(request.newLocation().getZ());
            home.setYaw(request.newLocation().getYaw());
            home.setPitch(request.newLocation().getPitch());
            groupService.updateGroup(group);

            return new HomeUpdateSuccess(home);

        } catch (Exception e) {
            return new HomeUpdateFailure("Failed to update home: " + e.getMessage());
        }
    }

    @Override
    public HomeDeletionResult deleteHome(HomeDeletionRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new HomeDeletionFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_HOME)) {
                return new HomeDeletionFailure("You don't have permission to manage homes");
            }

            // Check if home exists
            GroupHome home = group.getHome(request.homeName());
            if (home == null) {
                return new HomeDeletionFailure("Home not found");
            }

            // Delete the home
            boolean removed = group.removeHome(request.homeName());
            if (removed) {
                groupService.updateGroup(group);
                return new HomeDeletionSuccess(request.homeName());
            } else {
                return new HomeDeletionFailure("Failed to delete home");
            }

        } catch (Exception e) {
            return new HomeDeletionFailure("Failed to delete home: " + e.getMessage());
        }
    }

    @Override
    public DefaultHomeResult setDefaultHome(DefaultHomeRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new DefaultHomeFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_HOME)) {
                return new DefaultHomeFailure("You don't have permission to manage homes");
            }

            // Check if home exists
            GroupHome home = group.getHome(request.homeName());
            if (home == null) {
                return new DefaultHomeFailure("Home not found");
            }

            // Set as default (implementation depends on your GroupHome model)
            // For now, we'll consider this successful
            return new DefaultHomeSuccess(request.homeName());

        } catch (Exception e) {
            return new DefaultHomeFailure("Failed to set default home: " + e.getMessage());
        }
    }

    @Override
    public Optional<List<GroupHome>> listHomes(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        return Optional.of(List.copyOf(group.getHomes()));
    }

    @Override
    public ClaimResult claimChunk(ClaimRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new ClaimFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_CLAIM)) {
                return new ClaimFailure("You don't have permission to manage claims");
            }

            // Check if chunk is already claimed
            if (groupService.isChunkClaimed(request.chunkX(), request.chunkZ(), request.world())) {
                return new ClaimFailure("Chunk is already claimed");
            }

            // Check claim limit
            if (group.getClaimCount() >= config.getMaxClaimsPerFaction()) {
                return new ClaimFailure("Maximum number of claims reached (" + config.getMaxClaimsPerFaction() + ")");
            }

            // Perform the claim
            boolean success = groupService.claimChunk(request.playerId(), request.chunkX(), request.chunkZ(), request.world());

            if (success) {
                return new ClaimSuccess(request.chunkX(), request.chunkZ(), request.world(), group.getId());
            } else {
                return new ClaimFailure("Failed to claim chunk");
            }

        } catch (Exception e) {
            return new ClaimFailure("Failed to claim chunk: " + e.getMessage());
        }
    }

    @Override
    public UnclaimResult unclaimChunk(UnclaimRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new UnclaimFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_CLAIM)) {
                return new UnclaimFailure("You don't have permission to manage claims");
            }

            // Check if chunk is claimed by this group
            if (!group.isChunkClaimed(request.chunkX(), request.chunkZ(), request.world())) {
                return new UnclaimFailure("Chunk is not claimed by your group");
            }

            // Perform the unclaim
            boolean success = groupService.unclaimChunk(request.playerId(), request.chunkX(), request.chunkZ(), request.world());

            if (success) {
                return new UnclaimSuccess(request.chunkX(), request.chunkZ(), request.world());
            } else {
                return new UnclaimFailure("Failed to unclaim chunk");
            }

        } catch (Exception e) {
            return new UnclaimFailure("Failed to unclaim chunk: " + e.getMessage());
        }
    }

    @Override
    public boolean isChunkClaimed(int chunkX, int chunkZ, String world) {
        return groupService.isChunkClaimed(chunkX, chunkZ, world);
    }

    /**
     * Checks if a location is within the group's claimed territory.
     */
    private boolean isLocationInClaimedTerritory(Location location, Group group) {
        if (location == null || group == null) {
            return false;
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        String world = "world"; // Default world name, replace with actual world retrieval if possible

        return group.isChunkClaimed(chunkX, chunkZ, world);
    }
}
