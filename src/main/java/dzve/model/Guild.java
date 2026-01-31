package dzve.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Guild extends Group {

    @Builder.Default
    @JsonProperty("level")
    private int level = 1;

    @Builder.Default
    @JsonProperty("moneyToNextLevel")
    private double moneyToNextLevel = 0.0;

    public Guild(String name, String tag, String description, String color, PlayerRef player) {
        super(name, tag, description, color, player);
    }

    @Override
    public GroupType getType() {
        return GroupType.GUILD;
    }

    @Override
    public void addMember(PlayerRef player, UUID roleId) {
        super.addMember(player, roleId);

    }

    @Override
    public boolean removeMember(UUID playerId) {
        return super.removeMember(playerId);
    }

    public boolean canUpgrade() {
        return level < Arrays.stream(dzve.service.group.GroupService.getConfig().getGuildLevels()).max().orElse(1)
                && getBankBalance() >= calculateCostToNextLevel();
    }

    public double calculateCostToNextLevel() {
        return dzve.service.group.GroupService.getConfig().getInitialPrice()
                * Math.pow(dzve.service.group.GroupService.getConfig().getLevelPriceMultiplier(), level + 1);
    }

    @Override
    public Group copy() {
        Guild copy = new Guild();
        super.copyTo(copy);
        copy.setLevel(this.level);
        copy.setMoneyToNextLevel(this.moneyToNextLevel);
        return copy;
    }
}
