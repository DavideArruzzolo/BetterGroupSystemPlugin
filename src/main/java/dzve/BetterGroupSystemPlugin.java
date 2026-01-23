package dzve;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dzve.command.GroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;
import dzve.service.group.GroupServiceFactory;

import javax.annotation.Nonnull;


public class BetterGroupSystemPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Config<BetterGroupSystemPluginConfig> config;
    private GroupServiceFactory groupServiceFactory;

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
        GroupService.getInstance();
        loadConfig();
        this.groupServiceFactory = new GroupServiceFactory(config.get());
        this.getCommandRegistry().registerCommand(new GroupCommand(groupServiceFactory));
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutdown up plugin " + this.getName());
        if (groupServiceFactory != null) {
            groupServiceFactory.shutdown();
        }
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
