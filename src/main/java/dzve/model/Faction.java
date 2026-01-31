package dzve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Faction extends Group {

    @Builder.Default
    @JsonProperty("totalPower")
    private double totalPower = 0.0;

    @Builder.Default
    @JsonProperty("kills")
    private int kills = 0;

    @Builder.Default
    @JsonProperty("deaths")
    private int deaths = 0;

    @Builder.Default
    @JsonProperty("raidable")
    private boolean raidable = false;

    @Builder.Default
    @JsonProperty("lastRaidNotificationTimestamp")
    private long lastRaidNotificationTimestamp = 0;

    public Faction(String name, String tag, String description, String color, PlayerRef player) {
        super(name, tag, description, color, player);
    }

    @Override
    public GroupType getType() {
        return GroupType.FACTION;
    }

    @Override
    public void addMember(PlayerRef player, UUID roleId) {
        super.addMember(player, roleId);

        GroupMember member = getMember(player.getUuid());
        if (member != null) {
            member.setPower(dzve.service.group.GroupService.getConfig().getPlayerInitialPower());
        }
        recalculateTotalPower();
    }

    @Override
    public boolean removeMember(UUID playerId) {
        boolean removed = super.removeMember(playerId);
        if (removed) {
            recalculateTotalPower();
        }
        return removed;
    }

    public double getPlayerPower(UUID playerId) {
        GroupMember member = getMember(playerId);
        return member != null ? member.getPower() : 0.0;
    }

    public void addPlayerPower(UUID playerId, double amount) {
        GroupMember member = getMember(playerId);
        if (member != null) {
            member.setPower(member.getPower() + amount);
            recalculateTotalPower();
        }
    }

    public void removePlayerPower(UUID playerId, double amount) {
        GroupMember member = getMember(playerId);
        if (member != null) {
            member.setPower(member.getPower() - amount);
            recalculateTotalPower();
        }
    }

    public void recalculateTotalPower() {
        this.totalPower = getMembers().stream()
                .mapToDouble(GroupMember::getPower)
                .sum();
        if (this.totalPower < 0) {
            this.totalPower = 0;
        }
        updateRaidableStatus();
    }

    public void updateRaidableStatus() {
        this.raidable = getClaims().size() > getMaxClaims(dzve.service.group.GroupService.getConfig().getClaimRatio());
    }

    public void incrementKills() {
        this.kills++;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public int getMaxClaims(double claimRatio) {
        return (int) Math.max(0, totalPower * claimRatio);
    }

    public double getKillDeathRatio() {
        if (deaths == 0) {
            return kills > 0 ? Double.POSITIVE_INFINITY : 0.0;
        } else {
            return (double) kills / deaths;
        }
    }

    @Override
    public Group copy() {
        Faction copy = new Faction();
        super.copyTo(copy);
        copy.setTotalPower(this.totalPower);

        copy.setKills(this.kills);
        copy.setDeaths(this.deaths);
        copy.setRaidable(this.raidable);
        copy.setLastRaidNotificationTimestamp(this.lastRaidNotificationTimestamp);
        return copy;
    }
}