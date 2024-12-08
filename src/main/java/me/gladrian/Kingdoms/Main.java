package me.gladrian.Kingdoms;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private Config _config;
    private KingdomsDAL _dal;
    private KingdomsDataCache _cache;
    private TaskScheduler _scheduler;
    private TeamManager _teamManager;

    @Override
    public void onEnable() {
        _config = loadConfig();
        _dal = new KingdomsDAL(this.getDataFolder().getAbsolutePath());
        _cache = _dal.loadData();
        _teamManager = new TeamManager();

        this.getCommand("k").setExecutor(new CommandCore(_config, _dal, _cache, _teamManager));
        getServer().getPluginManager().registerEvents(new EventListener(_config, _dal, _cache, _teamManager), this);
        _scheduler = new TaskScheduler(this, _config, _dal, _cache);

        _teamManager.refreshTeamPrefixes(_cache.Kingdoms);

        getLogger().info("Kingdoms successfully enabled");
    }
    @Override
    public void onDisable() {
        _dal.saveAllData(_cache);
        getLogger().info("Disabled Kingdoms");
    }

    private Config loadConfig() {
        Config.DefineDefaults(getConfig());

        getConfig().options().copyDefaults(true);
        saveConfig();

        return new Config(getConfig());
    }
}
