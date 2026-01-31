package dzve;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dzve.command.BaseGroupCommand;
import dzve.command.chat.ChatAllayCommand;
import dzve.command.chat.ChatGroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.listener.ChatListener;
import dzve.listener.MapPlayerListener;
import dzve.service.group.GroupService;
import dzve.systems.DamageTrackerSystem;
import dzve.systems.PowerDeathSystem;
import dzve.systems.PvPProtectionSystem;
import dzve.systems.claim.ClaimAlertSystem;
import dzve.systems.claim.ClaimProtectionSystems;
import dzve.utils.LogService;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;

@Getter
public class BetterGroupSystemPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GroupService groupService;
    @Getter
    private static BetterGroupSystemPlugin instance;
    private final Config<BetterGroupSystemPluginConfig> config;
    private BaseGroupCommand baseGroupCommand;
    private MapPlayerListener mapPlayerListener;

    public BetterGroupSystemPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("Config", BetterGroupSystemPluginConfig.CODEC);
        loadConfig();
    }

    @Override
    protected void setup() {
        LogService.updateConfig(config.get());
        LogService.info("PLUGIN", "Setting up plugin " + this.getName());
        LogService.info("PLUGIN", "Plugin mode: " + config.get().getPluginMode());

        GroupService.initialize(config.get());
        groupService = GroupService.getInstance();
        baseGroupCommand = new BaseGroupCommand(config.get());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.PlaceBlockProtectionSystem());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.BreakBlockProtectionSystem());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.UseBlockProtectionSystem());

        this.getCommandRegistry().registerCommand(baseGroupCommand);
        this.getCommandRegistry().registerCommand(new ChatGroupCommand(groupService));
        this.getCommandRegistry().registerCommand(new ChatAllayCommand(groupService));
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new ClaimAlertSystem());
        this.getEntityStoreRegistry().registerSystem(new PvPProtectionSystem(config));
        this.getEntityStoreRegistry().registerSystem(new DamageTrackerSystem());
        this.getEntityStoreRegistry().registerSystem(new PowerDeathSystem());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.DamageBlockProtectionSystem());
        this.mapPlayerListener = new MapPlayerListener();
        EventRegistry eventRegistry = this.getEventRegistry();
        MapPlayerListener player = this.mapPlayerListener;
        Objects.requireNonNull(player);
        eventRegistry.registerGlobal(PlayerReadyEvent.class, player::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerChatEvent.class, ChatListener::onPlayerChat);

    }

    @Override
    protected void shutdown() {
        if (groupService != null) {
            groupService.shutdown();
        }
        LogService.info("PLUGIN", "Shutdown up plugin " + this.getName());
    }

    public void saveConfig() {
        try {
            config.save();
            LogService.info("CONFIG", "Configuration saved successfully");
        } catch (Exception e) {
            LogService.error("CONFIG", "Failed to save configuration", e);
        }
    }

    public void loadConfig() {
        try {
            config.load();
            LogService.updateConfig(config.get());
            LogService.info("CONFIG", "Configuration loaded successfully");
            saveConfig();
        } catch (Exception e) {
            LogService.error("CONFIG", "Failed to load configuration", e);
        }
    }
}
