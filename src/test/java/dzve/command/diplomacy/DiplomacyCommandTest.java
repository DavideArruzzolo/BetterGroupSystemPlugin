package dzve.command.diplomacy;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.model.DiplomacyStatus;
import dzve.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiplomacyCommandTest {

    @Mock
    private GroupService groupService;

    @Mock
    private CommandContext ctx;

    @Mock
    private PlayerRef player;

    @Mock
    private RequiredArg<String> targetGroupArg;

    @Mock
    private RequiredArg<String> statusArg;

    // We can't use @InjectMocks easily because of the super() call in constructor
    // and final fields.
    // We will instantiate manually and use reflection to set fields.
    private DiplomacyCommand command;

    @BeforeEach
    void setUp() throws Exception {
        // Since AbstractPlayerCommand constructor might do things we can't easily avoid
        // (like registering),
        // and we can't Mock the parent constructor.
        // However, usually pure logic tests can just instantiate it if the super
        // constructor doesn't crash.
        // The AbstractPlayerCommand constructor takes name/desc. It's likely fine.
        // BUT it calls `withRequiredArg` which might crash or return a real Arg.
        // We will try to instantiate it. If it fails, we might need a workaround.
        // Assuming it works for now (standard unit testing of logic).

        try {
            command = new DiplomacyCommand(groupService);
        } catch (Exception e) {
            // If instantiation fails due to Hytale internals in super(), we might need to
            // mock the command class itself
            // but call the real 'execute' method.
            // For now, let's assume instantiation is safe or we'll handle it if build
            // fails.
            throw new RuntimeException(
                    "Failed to instantiate command. Hytale JAR dependencies might be issues in unit tests env.", e);
        }

        // Inject mock args via reflection
        setField(command, "targetGroup", targetGroupArg);
        setField(command, "status", statusArg);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testExecute_Ally() {
        when(targetGroupArg.get(ctx)).thenReturn("OtherFaction");
        when(statusArg.get(ctx)).thenReturn("ALLY");

        command.execute(ctx, null, null, player, null);

        verify(groupService).setDiplomacy(player, "OtherFaction", DiplomacyStatus.ALLY);
    }

    @Test
    void testExecute_Neutral() {
        when(targetGroupArg.get(ctx)).thenReturn("EnemyFaction");
        when(statusArg.get(ctx)).thenReturn("NEUTRAL");

        command.execute(ctx, null, null, player, null);

        verify(groupService).setDiplomacy(player, "EnemyFaction", DiplomacyStatus.NEUTRAL);
    }

    @Test
    void testExecute_Enemy() {
        when(targetGroupArg.get(ctx)).thenReturn("TargetFaction");
        when(statusArg.get(ctx)).thenReturn("ENEMY");

        command.execute(ctx, null, null, player, null);

        verify(groupService).setDiplomacy(player, "TargetFaction", DiplomacyStatus.ENEMY);
    }

    @Test
    void testExecute_InvalidStatus() {
        when(statusArg.get(ctx)).thenReturn("INVALID_STATUS");

        command.execute(ctx, null, null, player, null);

        // Verification: ensure setDiplomacy is NOT called
        verify(groupService, org.mockito.Mockito.never()).setDiplomacy(any(), any(), any());

        // Optionally verify generic message sent to player if possible,
        // but Message.raw might be static and hard to mock without static mocking.
    }
}
