package dzve.model;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor // Necessario per Jackson/Gson a volte, e per il Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Importante: usiamo solo l'ID per l'uguaglianza
public class GroupHome {
    private UUID id;
    private String name;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
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
}
