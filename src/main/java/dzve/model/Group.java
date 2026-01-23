package dzve.model;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.config.BetterGroupSystemPluginConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Group {

    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Builder.Default
    private GroupType type = GroupType.valueOf(BetterGroupSystemPluginConfig.getInstance().getPluginMode());

    private String name;
    private String tag;
    private String description;
    private String color;
    private UUID leaderId;

    @Builder.Default
    private Set<GroupHome> homes = new HashSet<>();

    @Builder.Default
    private Set<GroupClaimedChunk> claims = new HashSet<>();

    @Builder.Default
    private Set<GroupMember> members = new HashSet<>();

    @Builder.Default
    private Set<GroupRole> roles = GroupRole.initializeRoles();

    @Builder.Default
    private double bankBalance = 0.0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private Map<UUID, DiplomacyStatus> diplomaticRelations = new HashMap<>();

    public Group(String name, String tag, String description, String color, PlayerRef player) {
        this();
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.color = color;
        this.leaderId = player.getUuid();

        addMember(player, roles.stream().findFirst().orElseThrow().getId());
    }

    public void addMember(PlayerRef player, UUID roleId) {
        GroupMember member = new GroupMember(player.getUuid(), player.getUsername(), roleId);
        members.add(member);
    }

    public boolean removeMember(UUID playerId) {
        return members.removeIf(member -> member.getPlayerId().equals(playerId));
    }

    public GroupMember getMember(UUID playerId) {
        return members.stream()
                .filter(member -> member.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isMember(UUID playerId) {
        return members.stream().anyMatch(member -> member.getPlayerId().equals(playerId));
    }

    public boolean isLeader(UUID playerId) {
        return leaderId != null && leaderId.equals(playerId);
    }


    public boolean changeMemberRole(UUID playerId, UUID newRoleId) {
        GroupMember member = getMember(playerId);
        if (member != null) {
            member.setRoleId(newRoleId);
            return true;
        }
        return false;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.bankBalance += amount;
        }
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && bankBalance >= amount) {
            this.bankBalance -= amount;
            return true;
        }
        return false;
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getClaimCount() {
        return claims.size();
    }

    public int getHomeCount() {
        return homes.size();
    }

    public void setDiplomacyStatus(UUID group, DiplomacyStatus status) {
        diplomaticRelations.put(group, status);
    }

    public DiplomacyStatus getDiplomacyStatus(UUID group) {
        return diplomaticRelations.getOrDefault(group, DiplomacyStatus.NEUTRAL);
    }

    public boolean isEnemy(UUID group) {
        return getDiplomacyStatus(group) == DiplomacyStatus.ENEMY;
    }

    public boolean isAlly(UUID group) {
        return getDiplomacyStatus(group) == DiplomacyStatus.ALLY;
    }

    public void addHome(GroupHome home) {
        homes.add(home);
    }

    public boolean removeHome(String homeName) {
        return homes.removeIf(home -> home.getName().equals(homeName));
    }

    public GroupHome getHome(String homeName) {
        return homes.stream()
                .filter(home -> home.getName().equals(homeName))
                .findFirst()
                .orElse(null);
    }

    public void addClaim(GroupClaimedChunk claim) {
        claims.add(claim);
    }

    public boolean removeClaim(int chunkX, int chunkZ, String world) {
        return claims.removeIf(claim ->
                claim.getChunkX() == chunkX &&
                        claim.getChunkZ() == chunkZ &&
                        claim.getWorld().equals(world));
    }

    public boolean isChunkClaimed(int chunkX, int chunkZ, String world) {
        return claims.stream().anyMatch(claim ->
                claim.getChunkX() == chunkX &&
                        claim.getChunkZ() == chunkZ &&
                        claim.getWorld().equals(world)
        );
    }
}
