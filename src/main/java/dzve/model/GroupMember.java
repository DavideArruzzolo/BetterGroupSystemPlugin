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
    private boolean online;
    private UUID groupId;
    private LocalDateTime lastActive;
    private UUID defaultHome;


    public GroupMember(UUID playerId, UUID roleId) {
        this.playerId = playerId;
        this.roleId = roleId;
        this.joinDate = LocalDateTime.now();
    }
}
