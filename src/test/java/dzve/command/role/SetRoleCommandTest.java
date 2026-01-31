package dzve.command.role;

import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import dzve.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetRoleCommandTest {

    @Mock
    private GroupService groupService;

    @Mock
    private CommandContext ctx;

    @Mock
    private RequiredArg<ProfileServiceClient.PublicGameProfile> targetArg;

    @Mock
    private RequiredArg<String> roleNameArg;

    @Mock
    private ProfileServiceClient.PublicGameProfile targetProfile;

    private SetRoleCommand command;

    @BeforeEach
    void setUp() throws Exception {
        try {
            command = new SetRoleCommand(groupService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate command", e);
        }

        setField(command, "target", targetArg);
        setField(command, "roleName", roleNameArg);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testExecute() {
        UUID targetId = UUID.randomUUID();
        when(targetArg.get(ctx)).thenReturn(targetProfile);
        when(targetProfile.getUuid()).thenReturn(targetId);
        when(roleNameArg.get(ctx)).thenReturn("Officer");

        command.execute(ctx, null, null, null, null);

        verify(groupService).setRole(null, targetId, "Officer");
    }
}
