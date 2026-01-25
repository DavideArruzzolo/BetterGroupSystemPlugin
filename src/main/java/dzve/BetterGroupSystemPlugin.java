package dzve;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dzve.command.BaseGroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.listener.GroupTracker;
import dzve.service.group.GroupService;
import dzve.systems.claim.ClaimAlertSystem;
import dzve.systems.claim.ClaimProtectionSystems;
import dzve.systems.claim.PvPProtectionSystem;
import lombok.Getter;

import javax.annotation.Nonnull;


@Getter
public class BetterGroupSystemPlugin extends JavaPlugin {
    private GroupTracker groupTracker;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GroupService groupService;
    private final Config<BetterGroupSystemPluginConfig> config;
    @Getter
    private static BetterGroupSystemPlugin instance;
    private BaseGroupCommand baseGroupCommand;

    public BetterGroupSystemPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("Config", BetterGroupSystemPluginConfig.CODEC);
        loadConfig();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        LOGGER.atInfo().log("Plugin mode: " + config.get().getPluginMode());
        baseGroupCommand = new BaseGroupCommand(config.get());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.PlaceBlockProtectionSystem());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.BreakBlockProtectionSystem());
        this.getEntityStoreRegistry().registerSystem(new ClaimProtectionSystems.UseBlockProtectionSystem());

        this.getCommandRegistry().registerCommand(baseGroupCommand);
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new ClaimAlertSystem());
        this.getEntityStoreRegistry().registerSystem(new PvPProtectionSystem(config));
        this.groupTracker = new GroupTracker();
        this.groupTracker.start();
    }

    @Override
    protected void shutdown() {
        if (this.groupTracker != null) {
            this.groupTracker.stop();
        }
        groupService.shutdown();
        LOGGER.atInfo().log("Shutdown up plugin " + this.getName());
    }

    public void saveConfig() {
        try {
            config.save();
            LOGGER.atInfo().log("Configuration saved successfully");
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to save configuration: " + e.getMessage());
        }
    }

    public void loadConfig() {
        try {
            config.load();
            LOGGER.atInfo().log("Configuration load successfully");
            saveConfig();
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to load configuration: " + e.getMessage());
        }
    }

    public GroupTracker getTracker() {
        return this.groupTracker;
    }
}
