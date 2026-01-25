package dzve;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dzve.command.BaseGroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;
import lombok.Getter;

import javax.annotation.Nonnull;


@Getter
public class BetterGroupSystemPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GroupService groupService;
    private final Config<BetterGroupSystemPluginConfig> config;
    private static BetterGroupSystemPlugin instance;
    private BaseGroupCommand baseGroupCommand;

    public BetterGroupSystemPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("Config", BetterGroupSystemPluginConfig.CODEC);
        loadConfig();
    }

    public static BetterGroupSystemPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        LOGGER.atInfo().log("Plugin mode: " + config.get().getPluginMode());
        groupService = GroupService.getInstance(config.get());
        baseGroupCommand = new BaseGroupCommand(config.get());
        this.getCommandRegistry().registerCommand(baseGroupCommand);
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
}
