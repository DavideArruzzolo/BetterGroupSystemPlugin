package dzve.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Guild.class, name = "GUILD"),
        @JsonSubTypes.Type(value = Faction.class, name = "FACTION")
})
public abstract class Group {

    @EqualsAndHashCode.Include
    @Builder.Default
    @JsonProperty("id")
    private UUID id = UUID.randomUUID();

    @JsonProperty("name")
    private String name;
    @JsonProperty("tag")
    private String tag;
    @JsonProperty("description")
    private String description;
    @JsonProperty("color")
    private String color;
    @JsonProperty("leaderId")
    private UUID leaderId;

    @Builder.Default
    @JsonProperty("homes")
    private Set<GroupHome> homes = new HashSet<>();

    @Builder.Default
    @JsonProperty("claims")
    private Set<GroupClaimedChunk> claims = new HashSet<>();

    @Builder.Default
    @JsonProperty("members")
    private Set<GroupMember> members = new HashSet<>();

    @Builder.Default
    @JsonProperty("roles")
    private Set<GroupRole> roles = new HashSet<>(GroupRole.initializeRoles());

    @Builder.Default
    @JsonProperty("bankBalance")
    private double bankBalance = 0.0;

    @Builder.Default
    @JsonProperty("createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @JsonProperty("diplomaticRelations")
    private Map<UUID, DiplomacyStatus> diplomaticRelations = new HashMap<>();

    public Group(String name, String tag, String description, String color, PlayerRef player) {
        this();
        this.name = name;
        this.tag = tag;
        this.description = description;
        if (color == null) {
            Random random = new Random();
            int nextInt = random.nextInt(0xffffff + 1);
            this.color = String.format("#%06x", nextInt);
        } else {
            this.color = color;
        }
        this.leaderId = player.getUuid();

        addMember(player,
                roles.stream().max(java.util.Comparator.comparingInt(GroupRole::getPriority)).orElseThrow().getId());
    }

    @JsonIgnore
    public abstract GroupType getType();

    public void addMember(PlayerRef player, UUID roleId) {
        GroupMember member = new GroupMember(player.getUuid(), player.getUsername(), roleId);
        members.add(member);
    }

    public boolean removeMember(UUID playerId) {
        GroupMember member = getMember(playerId);
        if (member != null) {
            this.bankBalance += member.getBankBalance();
            return members.remove(member);
        }
        return false;
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

    public void changeMemberRole(UUID playerId, UUID newRoleId) {
        GroupMember member = getMember(playerId);
        member.setRoleId(newRoleId);
    }

    public void deposit(double amount, UUID playerId) {
        if (amount > 0) {
            GroupMember member = getMember(playerId);
            if (member != null) {
                member.setBankBalance(member.getBankBalance() + amount);
            }
        }
    }

    public void withdraw(double amount, UUID playerId) {
        if (amount > 0) {
            GroupMember member = getMember(playerId);
            if (member != null && member.getBankBalance() >= amount) {
                member.setBankBalance(member.getBankBalance() - amount);
            }
        }
    }

    public double getBalance(UUID playerId) {
        GroupMember member = getMember(playerId);
        return (member != null) ? member.getBankBalance() : 0.0;
    }

    public void depositToGroup(double amount) {
        if (amount > 0) {
            this.bankBalance += amount;
        }
    }

    public boolean withdrawFromGroup(double amount, UUID playerId) {
        if (amount > 0 && bankBalance >= amount) {
            if (isLeader(playerId)) {
                this.bankBalance -= amount;
                return true;
            }
            GroupMember member = getMember(playerId);
            if (member != null) {
                GroupRole role = getRole(member.getRoleId());
                if (role != null && role.hasPermission(Permission.CAN_MANAGE_BANK)) {
                    this.bankBalance -= amount;
                    return true;
                }
            }
        }
        return false;
    }

    public GroupRole getRole(UUID roleId) {
        return roles.stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst()
                .orElse(null);
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

    public GroupHome getHomeById(UUID id) {
        return homes.stream()
                .filter(home -> home.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addClaim(GroupClaimedChunk claim) {
        claims.add(claim);
    }

    public void removeClaim(int chunkX, int chunkZ, String worldName) {
        claims.removeIf(claim -> claim.getChunkX() == chunkX &&
                claim.getChunkZ() == chunkZ &&
                claim.getWorld().equals(worldName));
    }

    public boolean isChunkClaimed(int chunkX, int chunkZ, String worldName) {
        return claims.stream().anyMatch(claim -> claim.getChunkX() == chunkX &&
                claim.getChunkZ() == chunkZ &&
                claim.getWorld().equals(worldName));
    }

    public abstract Group copy();

    protected void copyTo(Group target) {
        target.setId(this.id);
        target.setName(this.name);
        target.setTag(this.tag);
        target.setDescription(this.description);
        target.setColor(this.color);
        target.setLeaderId(this.leaderId);
        target.setBankBalance(this.bankBalance);
        target.setCreatedAt(this.createdAt);

        target.setHomes(this.homes.stream().map(GroupHome::copy).collect(java.util.stream.Collectors.toSet()));
        target.setClaims(
                this.claims.stream().map(GroupClaimedChunk::copy).collect(java.util.stream.Collectors.toSet()));
        target.setMembers(this.members.stream().map(GroupMember::copy).collect(java.util.stream.Collectors.toSet()));
        target.setRoles(this.roles.stream().map(GroupRole::copy).collect(java.util.stream.Collectors.toSet()));
        target.setDiplomaticRelations(new HashMap<>(this.diplomaticRelations));
    }
}