package dzve.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupClaimedChunk {
    @EqualsAndHashCode.Include
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("chunkX")
    private int chunkX;
    @JsonProperty("chunkZ")
    private int chunkZ;
    @JsonProperty("world")
    private String world;

    public GroupClaimedChunk(int chunkX, int chunkZ, String world) {
        this.id = UUID.randomUUID();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.world = world;
    }

    public GroupClaimedChunk copy() {
        return GroupClaimedChunk.builder()
                .id(this.id)
                .chunkX(this.chunkX)
                .chunkZ(this.chunkZ)
                .world(this.world)
                .build();
    }
}
