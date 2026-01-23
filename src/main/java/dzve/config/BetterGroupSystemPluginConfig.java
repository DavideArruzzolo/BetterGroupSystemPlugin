package dzve.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Data;

import static com.hypixel.hytale.codec.Codec.*;

@Data
public class BetterGroupSystemPluginConfig {
    public static final String MOD_NAME = "[BetterGroupPlugin]";
    public static final String DATA_FOLDER = "mods/Dzve_BetterGroupSystemPlugin/data";
    public static final String FILE_NAME = "groups.json";

    public static final BuilderCodec<BetterGroupSystemPluginConfig> CODEC =
            BuilderCodec.builder(BetterGroupSystemPluginConfig.class, BetterGroupSystemPluginConfig::new)
                    .append(new KeyedCodec<>("PluginMode", STRING), (cfg, val, info) -> cfg.pluginMode = val, (cfg, info) -> cfg.pluginMode).add()
                    .append(new KeyedCodec<>("AllowedWorlds", STRING), (cfg, val, info) -> cfg.allowedWorlds = val, (cfg, info) -> cfg.allowedWorlds).add()
                    .append(new KeyedCodec<>("AllCommandsPrefix", STRING), (cfg, val, info) -> cfg.allCommandsPrefix = val, (cfg, info) -> cfg.allCommandsPrefix).add()
                    .append(new KeyedCodec<>("HidePlayers", BOOLEAN), (cfg, val, info) -> cfg.hidePlayers = val, (cfg, info) -> cfg.hidePlayers).add()
                    .append(new KeyedCodec<>("MaxSize", INTEGER), (cfg, val, info) -> cfg.maxSize = val, (cfg, info) -> cfg.maxSize).add()
                    .append(new KeyedCodec<>("MinNameLength", INTEGER), (cfg, val, info) -> cfg.minNameLength = val, (cfg, info) -> cfg.minNameLength).add()
                    .append(new KeyedCodec<>("MaxNameLength", INTEGER), (cfg, val, info) -> cfg.maxNameLength = val, (cfg, info) -> cfg.maxNameLength).add()
                    .append(new KeyedCodec<>("MinTagLength", INTEGER), (cfg, val, info) -> cfg.minTagLength = val, (cfg, info) -> cfg.minTagLength).add()
                    .append(new KeyedCodec<>("MaxTagLength", INTEGER), (cfg, val, info) -> cfg.maxTagLength = val, (cfg, info) -> cfg.maxTagLength).add()
                    .append(new KeyedCodec<>("MaxDescriptionLength", INTEGER), (cfg, val, info) -> cfg.maxDescriptionLength = val, (cfg, info) -> cfg.maxDescriptionLength).add()
                    .append(new KeyedCodec<>("DisablePowerSystemDetails", BOOLEAN), (cfg, val, info) -> cfg.disablePowerSystemDetails = val, (cfg, info) -> cfg.disablePowerSystemDetails).add()
                    .append(new KeyedCodec<>("PlayerInitialPower", DOUBLE), (cfg, val, info) -> cfg.playerInitialPower = val, (cfg, info) -> cfg.playerInitialPower).add()
                    .append(new KeyedCodec<>("PlayerPowerMax", DOUBLE), (cfg, val, info) -> cfg.playerPowerMax = val, (cfg, info) -> cfg.playerPowerMax).add()
                    .append(new KeyedCodec<>("PlayerPowerMin", DOUBLE), (cfg, val, info) -> cfg.playerPowerMin = val, (cfg, info) -> cfg.playerPowerMin).add()
                    .append(new KeyedCodec<>("PowerGainByKill", DOUBLE), (cfg, val, info) -> cfg.powerGainByKill = val, (cfg, info) -> cfg.powerGainByKill).add()
                    .append(new KeyedCodec<>("PowerGainByTime", DOUBLE), (cfg, val, info) -> cfg.powerGainByTime = val, (cfg, info) -> cfg.powerGainByTime).add()
                    .append(new KeyedCodec<>("PowerLooseByDeath", DOUBLE), (cfg, val, info) -> cfg.powerLooseByDeath = val, (cfg, info) -> cfg.powerLooseByDeath).add()
                    .append(new KeyedCodec<>("PowerRegenOffline", DOUBLE), (cfg, val, info) -> cfg.powerRegenOffline = val, (cfg, info) -> cfg.powerRegenOffline).add()
                    .append(new KeyedCodec<>("ClaimRatio", DOUBLE), (cfg, val, info) -> cfg.claimRatio = val, (cfg, info) -> cfg.claimRatio).add()
                    .append(new KeyedCodec<>("MaxClaimsPerFaction", INTEGER), (cfg, val, info) -> cfg.maxClaimsPerFaction = val, (cfg, info) -> cfg.maxClaimsPerFaction).add()
                    .append(new KeyedCodec<>("GuildLevels", INT_ARRAY), (cfg, val, info) -> cfg.guildLevels = val, (cfg, info) -> cfg.guildLevels).add()
                    .append(new KeyedCodec<>("LevelPriceMultiplier", DOUBLE), (cfg, val, info) -> cfg.levelPriceMultiplier = val, (cfg, info) -> cfg.levelPriceMultiplier).add()
                    .append(new KeyedCodec<>("InitialPrice", DOUBLE), (cfg, val, info) -> cfg.initialPrice = val, (cfg, info) -> cfg.initialPrice).add()
                    .append(new KeyedCodec<>("SlotQuantityGainForLevel", INTEGER), (cfg, val, info) -> cfg.slotQuantityGainForLevel = val, (cfg, info) -> cfg.slotQuantityGainForLevel).add()
                    .append(new KeyedCodec<>("EnableTax", BOOLEAN), (cfg, val, info) -> cfg.enableTax = val, (cfg, info) -> cfg.enableTax).add()
                    .append(new KeyedCodec<>("TaxImport", DOUBLE), (cfg, val, info) -> cfg.taxImport = val, (cfg, info) -> cfg.taxImport).add()
                    .append(new KeyedCodec<>("TaxInterval", DOUBLE), (cfg, val, info) -> cfg.taxInterval = val, (cfg, info) -> cfg.taxInterval).add()
                    .append(new KeyedCodec<>("MaxLatePayment", INTEGER), (cfg, val, info) -> cfg.maxLatePayment = val, (cfg, info) -> cfg.maxLatePayment).add()
                    .append(new KeyedCodec<>("LatePayAction", STRING), (cfg, val, info) -> cfg.latePayAction = val, (cfg, info) -> cfg.latePayAction).add()
                    .append(new KeyedCodec<>("LatePayGracePeriod", DOUBLE), (cfg, val, info) -> cfg.latePayGracePeriod = val, (cfg, info) -> cfg.latePayGracePeriod).add()
                    .append(new KeyedCodec<>("MaxHome", INTEGER), (cfg, val, info) -> cfg.maxHome = val, (cfg, info) -> cfg.maxHome).add()
                    .append(new KeyedCodec<>("ChatMessageMaxLength", INTEGER), (cfg, val, info) -> cfg.chatMessageMaxLength = val, (cfg, info) -> cfg.chatMessageMaxLength)
                    .add().build();

    private static BetterGroupSystemPluginConfig instance;

    private String pluginMode = "FACTION";
    private String allowedWorlds = "*";
    private String allCommandsPrefix = "faction";
    private boolean hidePlayers = true;
    private int maxSize = 10;
    private int minNameLength = 3;
    private int maxNameLength = 15;
    private int minTagLength = 2;
    private int maxTagLength = 5;
    private int maxDescriptionLength = 255;
    private boolean disablePowerSystemDetails = true;
    private double playerInitialPower = 5;
    private double playerPowerMax = 100;
    private double playerPowerMin = -100;
    private double powerGainByKill = 1;
    private double powerGainByTime = 0.001;
    private double powerLooseByDeath = 1;
    private double powerRegenOffline = 0.0001;
    private double claimRatio = 0.5;
    private int maxClaimsPerFaction = 100;
    private int[] guildLevels = {1, 2, 3, 4, 5};
    private double initialPrice = 2500;
    private double levelPriceMultiplier = 1.2;
    private int slotQuantityGainForLevel = 10;
    private boolean enableTax = false;
    private double taxImport = 0.0;
    private double taxInterval = 86400;
    private int maxLatePayment = 5;
    private String latePayAction = "NONE";
    private double latePayGracePeriod = 28800;
    private int maxHome = 5;
    private int chatMessageMaxLength = 40;

    public BetterGroupSystemPluginConfig() {
    }

    public static BetterGroupSystemPluginConfig getInstance() {
        if (instance == null) {
            instance = new BetterGroupSystemPluginConfig();
        }
        return instance;
    }
}
