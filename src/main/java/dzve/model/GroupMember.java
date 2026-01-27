package dzve.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupMember {
    @JsonProperty("playerId")
    private UUID playerId;
    @JsonProperty("playerName")
    private String playerName;
    @JsonProperty("roleId")
    private UUID roleId;
    @JsonProperty("joinDate")
    private LocalDateTime joinDate;
    @JsonProperty("lastActive")
    private LocalDateTime lastActive;
    @JsonProperty("defaultHome")
    private UUID defaultHome;
    @JsonProperty("bankBalance")
    private double bankBalance;

    public GroupMember(UUID playerId, String playerName, UUID roleId) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.roleId = roleId;
        this.lastActive = LocalDateTime.now();
        this.joinDate = LocalDateTime.now();
    }
}
