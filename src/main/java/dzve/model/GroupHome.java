package dzve.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupHome {
    @EqualsAndHashCode.Include
    private UUID id;
    private String name;
    private UUID world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public GroupHome(String name, UUID world, double x, double y, double z, float yaw, float pitch) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
