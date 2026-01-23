package dzve;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dzve.command.BaseGroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;


public class BetterGroupSystemPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Config<BetterGroupSystemPluginConfig> config;
    private static GroupService groupService;

    public BetterGroupSystemPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
        this.config = this.withConfig("FactionConfig", BetterGroupSystemPluginConfig.CODEC);
        loadConfig();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        LOGGER.atInfo().log("Plugin mode: " + config.get().getPluginMode());
        groupService = GroupService.getInstance();
        loadConfig();
        this.getCommandRegistry().registerCommand(new BaseGroupCommand());
    }

    @Override
    protected void shutdown() {
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

    public Config<BetterGroupSystemPluginConfig> getConfig() {
        return config;
    }
}
