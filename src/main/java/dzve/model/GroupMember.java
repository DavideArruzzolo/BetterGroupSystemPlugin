package dzve.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupMember {
    private UUID playerId;
    private String playerName;
    private UUID roleId;
    private LocalDateTime joinDate;
    private LocalDateTime lastActive;
    private UUID defaultHome;
    private double bankBalance;


    public GroupMember(UUID playerId, String playerName, UUID roleId) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.roleId = roleId;
        this.lastActive = LocalDateTime.now();
        this.joinDate = LocalDateTime.now();
    }
}
