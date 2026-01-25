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
                    .append(new KeyedCodec<>("PluginMode", STRING), (cfg, val, info) -> cfg.setPluginMode(val), (cfg, info) -> cfg.getPluginMode()).add()
                    .append(new KeyedCodec<>("AllowedWorlds", STRING), (cfg, val, info) -> cfg.setAllowedWorlds(val), (cfg, info) -> cfg.getAllowedWorlds()).add()
                    .append(new KeyedCodec<>("AllCommandsPrefix", STRING), (cfg, val, info) -> cfg.setAllCommandsPrefix(val), (cfg, info) -> cfg.getAllCommandsPrefix()).add()
                    .append(new KeyedCodec<>("HidePlayers", BOOLEAN), (cfg, val, info) -> cfg.setHidePlayers(val), (cfg, info) -> cfg.isHidePlayers()).add()
                    .append(new KeyedCodec<>("MaxSize", INTEGER), (cfg, val, info) -> cfg.setMaxSize(val), (cfg, info) -> cfg.getMaxSize()).add()
                    .append(new KeyedCodec<>("MinNameLength", INTEGER), (cfg, val, info) -> cfg.setMinNameLength(val), (cfg, info) -> cfg.getMinNameLength()).add()
                    .append(new KeyedCodec<>("MaxNameLength", INTEGER), (cfg, val, info) -> cfg.setMaxNameLength(val), (cfg, info) -> cfg.getMaxNameLength()).add()
                    .append(new KeyedCodec<>("MinTagLength", INTEGER), (cfg, val, info) -> cfg.setMinTagLength(val), (cfg, info) -> cfg.getMinTagLength()).add()
                    .append(new KeyedCodec<>("MaxTagLength", INTEGER), (cfg, val, info) -> cfg.setMaxTagLength(val), (cfg, info) -> cfg.getMaxTagLength()).add()
                    .append(new KeyedCodec<>("MaxDescriptionLength", INTEGER), (cfg, val, info) -> cfg.setMaxDescriptionLength(val), (cfg, info) -> cfg.getMaxDescriptionLength()).add()
                    .append(new KeyedCodec<>("DisablePowerSystemDetails", BOOLEAN), (cfg, val, info) -> cfg.setDisablePowerSystemDetails(val), (cfg, info) -> cfg.isDisablePowerSystemDetails()).add()
                    .append(new KeyedCodec<>("PlayerInitialPower", DOUBLE), (cfg, val, info) -> cfg.setPlayerInitialPower(val), (cfg, info) -> cfg.getPlayerInitialPower()).add()
                    .append(new KeyedCodec<>("PlayerPowerMax", DOUBLE), (cfg, val, info) -> cfg.setPlayerPowerMax(val), (cfg, info) -> cfg.getPlayerPowerMax()).add()
                    .append(new KeyedCodec<>("PlayerPowerMin", DOUBLE), (cfg, val, info) -> cfg.setPlayerPowerMin(val), (cfg, info) -> cfg.getPlayerPowerMin()).add()
                    .append(new KeyedCodec<>("PowerGainByKill", DOUBLE), (cfg, val, info) -> cfg.setPowerGainByKill(val), (cfg, info) -> cfg.getPowerGainByKill()).add()
                    .append(new KeyedCodec<>("PowerGainByTime", DOUBLE), (cfg, val, info) -> cfg.setPowerGainByTime(val), (cfg, info) -> cfg.getPowerGainByTime()).add()
                    .append(new KeyedCodec<>("PowerLooseByDeath", DOUBLE), (cfg, val, info) -> cfg.setPowerLooseByDeath(val), (cfg, info) -> cfg.getPowerLooseByDeath()).add()
                    .append(new KeyedCodec<>("PowerRegenOffline", DOUBLE), (cfg, val, info) -> cfg.setPowerRegenOffline(val), (cfg, info) -> cfg.getPowerRegenOffline()).add()
                    .append(new KeyedCodec<>("ClaimRatio", DOUBLE), (cfg, val, info) -> cfg.setClaimRatio(val), (cfg, info) -> cfg.getClaimRatio()).add()
                    .append(new KeyedCodec<>("MaxClaimsPerFaction", INTEGER), (cfg, val, info) -> cfg.setMaxClaimsPerFaction(val), (cfg, info) -> cfg.getMaxClaimsPerFaction()).add()
                    .append(new KeyedCodec<>("GuildLevels", INT_ARRAY), (cfg, val, info) -> cfg.setGuildLevels(val), (cfg, info) -> cfg.getGuildLevels()).add()
                    .append(new KeyedCodec<>("LevelPriceMultiplier", DOUBLE), (cfg, val, info) -> cfg.setLevelPriceMultiplier(val), (cfg, info) -> cfg.getLevelPriceMultiplier()).add()
                    .append(new KeyedCodec<>("InitialPrice", DOUBLE), (cfg, val, info) -> cfg.setInitialPrice(val), (cfg, info) -> cfg.getInitialPrice()).add()
                    .append(new KeyedCodec<>("SlotQuantityGainForLevel", INTEGER), (cfg, val, info) -> cfg.setSlotQuantityGainForLevel(val), (cfg, info) -> cfg.getSlotQuantityGainForLevel()).add()
                    .append(new KeyedCodec<>("EnableTax", BOOLEAN), (cfg, val, info) -> cfg.setEnableTax(val), (cfg, info) -> cfg.isEnableTax()).add()
                    .append(new KeyedCodec<>("TaxImport", DOUBLE), (cfg, val, info) -> cfg.setTaxImport(val), (cfg, info) -> cfg.getTaxImport()).add()
                    .append(new KeyedCodec<>("TaxInterval", DOUBLE), (cfg, val, info) -> cfg.setTaxInterval(val), (cfg, info) -> cfg.getTaxInterval()).add()
                    .append(new KeyedCodec<>("MaxLatePayment", INTEGER), (cfg, val, info) -> cfg.setMaxLatePayment(val), (cfg, info) -> cfg.getMaxLatePayment()).add()
                    .append(new KeyedCodec<>("LatePayAction", STRING), (cfg, val, info) -> cfg.setLatePayAction(val), (cfg, info) -> cfg.getLatePayAction()).add()
                    .append(new KeyedCodec<>("LatePayGracePeriod", DOUBLE), (cfg, val, info) -> cfg.setLatePayGracePeriod(val), (cfg, info) -> cfg.getLatePayGracePeriod()).add()
                    .append(new KeyedCodec<>("MaxHome", INTEGER), (cfg, val, info) -> cfg.setMaxHome(val), (cfg, info) -> cfg.getMaxHome()).add()
                    .append(new KeyedCodec<>("ChatMessageMaxLength", INTEGER), (cfg, val, info) -> cfg.setChatMessageMaxLength(val), (cfg, info) -> cfg.getChatMessageMaxLength()).add()
                    .append(new KeyedCodec<>("PvpEnabled", BOOLEAN), (cfg, val, info) -> cfg.setPvpEnabled(val), (cfg, info) -> cfg.isPvpEnabled()).add().build();

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
    private boolean pvpEnabled = true;

    public BetterGroupSystemPluginConfig() {
    }

    public static BetterGroupSystemPluginConfig getInstance() {
        if (instance == null) {
            instance = new BetterGroupSystemPluginConfig();
        }
        return instance;
    }
}
