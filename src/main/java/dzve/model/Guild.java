package dzve.model;

import dzve.config.BetterGroupSystemPluginConfig;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Guild extends Group {

    @Builder.Default
    private int level = 1;

    @Builder.Default
    private double moneyToNextLevel = 0.0;

    @Builder.Default
    private Map<UUID, Double> moneyContributions = new HashMap<>();

    public Guild(String name, String tag, String description, String color, UUID leaderId) {
        super(name, tag, description, color, leaderId);
    }

    @Override
    public void addMember(UUID playerId, UUID roleId) {
        super.addMember(playerId, roleId);
        moneyContributions.put(playerId, 0.0);
    }

    @Override
    public boolean removeMember(UUID playerId) {
        return super.removeMember(playerId);
    }

    private void levelUp() {
        level++;
    }

    public boolean canUpgrade() {
        return level < Arrays.stream(BetterGroupSystemPluginConfig.getInstance().getGuildLevels()).max().orElse(1) && getBankBalance() >= calculateCostToNextLevel();
    }

    public double calculateCostToNextLevel() {
        return BetterGroupSystemPluginConfig.getInstance().getInitialPrice() * Math.pow(BetterGroupSystemPluginConfig.getInstance().getLevelPriceMultiplier(), level + 1);
    }

    public double calculateDifferenceToNextLevel() {
        return calculateCostToNextLevel() - getBankBalance();
    }
}
