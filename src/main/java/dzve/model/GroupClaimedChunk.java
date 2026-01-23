package dzve.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
