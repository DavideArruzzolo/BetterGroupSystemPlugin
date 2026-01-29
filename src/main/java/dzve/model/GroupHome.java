package dzve.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupHome {
    @EqualsAndHashCode.Include
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("world")
    private String world;
    @JsonProperty("x")
    private double x;
    @JsonProperty("y")
    private double y;
    @JsonProperty("z")
    private double z;
    @JsonProperty("yaw")
    private float yaw;
    @JsonProperty("pitch")
    private float pitch;

    public GroupHome(String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public GroupHome copy() {
        return GroupHome.builder()
                .id(this.id)
                .name(this.name)
                .world(this.world)
                .x(this.x)
                .y(this.y)
                .z(this.z)
                .yaw(this.yaw)
                .pitch(this.pitch)
                .build();
    }
}
