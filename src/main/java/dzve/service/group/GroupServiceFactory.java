package dzve.service.group;

import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.diplomacy.GroupDiplomacyService;
import dzve.service.group.economy.GroupEconomyService;
import dzve.service.group.management.GroupManagementService;
import dzve.service.group.member.GroupMemberService;
import dzve.service.group.role.GroupRoleService;
import dzve.service.group.territory.GroupTerritoryService;

/**
 * Factory class for managing group service instances.
 */
public final class GroupServiceFactory {

    private final GroupService coreGroupService;
    private final GroupManagementService managementService;
    private final GroupMemberService memberService;
    private final GroupRoleService roleService;
    private final GroupTerritoryService territoryService;
    private final GroupDiplomacyService diplomacyService;
    private final GroupEconomyService economyService;

    public GroupServiceFactory(BetterGroupSystemPluginConfig config) {
        this.coreGroupService = GroupService.getInstance();

        // Initialize all service implementations
        this.managementService = new GroupManagementServiceImpl(coreGroupService, config);
        this.memberService = new GroupMemberServiceImpl(coreGroupService, config);
        this.roleService = new GroupRoleServiceImpl(coreGroupService);
        this.territoryService = new GroupTerritoryServiceImpl(coreGroupService, config);
        this.diplomacyService = new GroupDiplomacyServiceImpl(coreGroupService);
        this.economyService = new GroupEconomyServiceImpl(coreGroupService, config);
    }

    /**
     * Gets the core group service.
     */
    public GroupService getCoreGroupService() {
        return coreGroupService;
    }

    /**
     * Gets the group management service.
     */
    public GroupManagementService getManagementService() {
        return managementService;
    }

    /**
     * Gets the group member service.
     */
    public GroupMemberService getMemberService() {
        return memberService;
    }

    /**
     * Gets the group role service.
     */
    public GroupRoleService getRoleService() {
        return roleService;
    }

    /**
     * Gets the group territory service.
     */
    public GroupTerritoryService getTerritoryService() {
        return territoryService;
    }

    /**
     * Gets the group diplomacy service.
     */
    public GroupDiplomacyService getDiplomacyService() {
        return diplomacyService;
    }

    /**
     * Gets the group economy service.
     */
    public GroupEconomyService getEconomyService() {
        return economyService;
    }

    /**
     * Shuts down all services. Call this during plugin shutdown.
     */
    public void shutdown() {
        coreGroupService.shutdown();
    }
}
