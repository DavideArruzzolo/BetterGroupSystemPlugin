package dzve.model;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.config.BetterGroupSystemPluginConfig;
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
public class Faction extends Group {

    @Builder.Default
    private double totalPower = 0.0;

    @Builder.Default
    private Map<UUID, Double> playerPower = new HashMap<>();

    @Builder.Default
    private int kills = 0;

    @Builder.Default
    private int deaths = 0;

    @Builder.Default
    private boolean raidable = false;

    public Faction(String name, String tag, String description, String color, PlayerRef player) {
        super(name, tag, description, color, player);

        playerPower.put(player.getUuid(), BetterGroupSystemPluginConfig.getInstance().getPlayerInitialPower());
        recalculateTotalPower();
    }

    @Override
    public void addMember(PlayerRef player, UUID roleId) {
        super.addMember(player, roleId);
        playerPower.put(player.getUuid(), BetterGroupSystemPluginConfig.getInstance().getPlayerInitialPower());
        recalculateTotalPower();
    }

    @Override
    public boolean removeMember(UUID playerId) {
        playerPower.remove(playerId);
        recalculateTotalPower();
        return super.removeMember(playerId);
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
        this.totalPower = playerPower.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        updateRaidableStatus();
    }

    private void updateRaidableStatus() {
        boolean wasRaidable = this.raidable;
        this.raidable = totalPower < 0;
    }

    public void incrementKills() {
        this.kills++;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public int getMaxClaims(double claimRatio) {
        return (int) Math.max(0, totalPower * BetterGroupSystemPluginConfig.getInstance().getClaimRatio());
    }

    public double getKillDeathRatio() {
        if (deaths == 0) return kills > 0 ? Double.POSITIVE_INFINITY : 0.0;
        return (double) kills / deaths;
    }
}
