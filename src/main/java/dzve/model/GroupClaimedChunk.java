package dzve.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // Necessario per Jackson/Gson a volte, e per il Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Importante: usiamo solo l'ID per l'uguaglianza
public class GroupClaimedChunk {
    private UUID id;
    private int chunkX;
    private int chunkZ;
    private String world;

    public GroupClaimedChunk(int chunkX, int chunkZ, String world) {
        this.id = UUID.randomUUID();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.world = world;
    }
}
