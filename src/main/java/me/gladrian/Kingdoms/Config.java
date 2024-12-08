package me.gladrian.Kingdoms;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public int InitialMoneyForNewKingdom;
    public int DiamondToMoneyRate;
    public double HourOfPlayTimeToMoneyRate;
    public int ChunkClaimCost;
    public String MoneyNamePlural;
    public List<String> SovereignTitleOptions;
    public int ChatPrefixCharacterLimit;
    public int BackupFrequencyHours;
    public int BackupRetentionDays;

    public Config(FileConfiguration pluginConfig) {
        ChunkClaimCost = pluginConfig.getInt("chunk-claim-cost");
        InitialMoneyForNewKingdom = pluginConfig.getInt("initial-money-for-new-kingdom");
        DiamondToMoneyRate = pluginConfig.getInt("diamond-to-money-exchange-rate");
        HourOfPlayTimeToMoneyRate = pluginConfig.getDouble("hour-of-playtime-to-money-rate");
        MoneyNamePlural = pluginConfig.getString("money-name-plural");
        ChatPrefixCharacterLimit = pluginConfig.getInt("chat-prefix-character-limit");
        SovereignTitleOptions = pluginConfig.getStringList("sovereign-title-options");
        BackupFrequencyHours = pluginConfig.getInt("backup-frequency-hours");
        BackupRetentionDays = pluginConfig.getInt("backup-retention-days");
    }

    public static void DefineDefaults(FileConfiguration pluginConfig) {
        pluginConfig.addDefault("chunk-claim-cost", 10);
        pluginConfig.addDefault("initial-money-for-new-kingdom", 100);
        pluginConfig.addDefault("diamond-to-money-exchange-rate", 2);
        pluginConfig.addDefault("hour-of-playtime-to-money-rate", 3.0);
        pluginConfig.addDefault("money-name-plural", "rubies");
        pluginConfig.addDefault("chat-prefix-character-limit", 10);
        pluginConfig.addDefault("sovereign-title-options",
                new String[] {"Sovereign", "King", "Queen"});
        pluginConfig.addDefault("backup-frequency-hours", 12);
        pluginConfig.addDefault("backup-retention-days", 30);
    }
}
