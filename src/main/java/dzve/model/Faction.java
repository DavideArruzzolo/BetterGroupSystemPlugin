package dzve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
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
    @JsonProperty("playerPower")
    private Map<UUID, Double> playerPower = new HashMap<>();

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
        if (playerPower == null) {
            playerPower = new HashMap<>();
        }
        playerPower.put(player.getUuid(), dzve.service.group.GroupService.getConfig().getPlayerInitialPower());
        recalculateTotalPower();
    }

    @Override
    public boolean removeMember(UUID playerId) {
        boolean removed = super.removeMember(playerId);
        if (removed) {
            if (playerPower != null) {
                playerPower.remove(playerId);
                recalculateTotalPower();
            }
        }
        return removed;
    }

    public double getPlayerPower(UUID playerId) {
        return playerPower.getOrDefault(playerId, 0.0);
    }

    public void addPlayerPower(UUID playerId, double amount) {
        if (isMember(playerId)) {
            double currentPower = playerPower.getOrDefault(playerId, 0.0);
            playerPower.put(playerId, currentPower + amount);
            recalculateTotalPower();
        }
    }

    public void removePlayerPower(UUID playerId, double amount) {
        if (isMember(playerId)) {
            double currentPower = playerPower.getOrDefault(playerId, 0.0);
            playerPower.put(playerId, currentPower - amount);
            recalculateTotalPower();
        }
    }

    private void recalculateTotalPower() {
        if (playerPower == null) {
            this.totalPower = 0.0;
        } else {
            this.totalPower = playerPower.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
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
        copy.setPlayerPower(new HashMap<>(this.playerPower));
        copy.setKills(this.kills);
        copy.setDeaths(this.deaths);
        copy.setRaidable(this.raidable);
        copy.setLastRaidNotificationTimestamp(this.lastRaidNotificationTimestamp);
        return copy;
    }
}