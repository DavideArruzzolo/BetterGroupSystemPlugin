package dzve.command.member;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitePlayerCommandTest {

    @Mock
    private GroupService groupService;

    @Mock
    private CommandContext ctx;

    @Mock
    private RequiredArg<PlayerRef> targetArg;

    private InvitePlayerCommand command;

    @BeforeEach
    void setUp() throws Exception {
        try {
            command = new InvitePlayerCommand(groupService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate command", e);
        }

        setField(command, "target", targetArg);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testExecute() {
        when(targetArg.get(ctx)).thenReturn(null);

        command.execute(ctx, null, null, null, null);

        verify(groupService).invitePlayer(null, null);
    }
}
