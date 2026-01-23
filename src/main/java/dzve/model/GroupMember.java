package dzve.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // Necessario per Jackson/Gson a volte, e per il Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Importante: usiamo solo l'ID per l'uguaglianza
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
