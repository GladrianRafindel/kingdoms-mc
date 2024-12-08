package me.gladrian.Kingdoms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class TaskScheduler {
    private final Config _config;
    private final KingdomsDAL _dal;
    private final KingdomsDataCache _cache;

    private final int _moneyIncrementIntervalMinutes = 5;
    private final HashMap<UUID, Integer> _lastKingdomEntered = new HashMap<>();

    public TaskScheduler(Plugin plugin, Config config, KingdomsDAL dal, KingdomsDataCache cache) {
        _config = config;
        _dal = dal;
        _cache = cache;

        BukkitScheduler scheduler = Bukkit.getScheduler();

        scheduler.runTaskTimer(plugin, this::incrementMoneyOfKingdoms,
                200L, 20L * 60L * _moneyIncrementIntervalMinutes);

        scheduler.runTaskTimer(plugin, this::displayTitleForKingdomEnterOrExit,
                200L, 20L);
    }

    private void incrementMoneyOfKingdoms() {
        for (var kingdom : _cache.Kingdoms) {
            int numMembersOnline = kingdom.getOnlineMembers().size();
            float hoursSinceLastIncrement = _moneyIncrementIntervalMinutes / 60f;
            kingdom.Money += hoursSinceLastIncrement * _config.HourOfPlayTimeToMoneyRate * numMembersOnline;
        }
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
    }

    private void displayTitleForKingdomEnterOrExit() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equalsIgnoreCase("world"))
                continue;

            var uuid = player.getUniqueId();
            var lastKingdomEnteredId = _lastKingdomEntered.getOrDefault(uuid, -1);

            var chunk = player.getLocation().getChunk();
            var currKingdom = _cache.getCurrentClaim(chunk.getX(), chunk.getZ());
            var currentKingdomId = currKingdom != null ? currKingdom.Id : -1;

            if (currentKingdomId != lastKingdomEnteredId) {
                // Say "Entering" when entering from unclaimed or another kingdom
                if (lastKingdomEnteredId == -1 || currentKingdomId != -1) {
                    player.sendTitle("", "Entering " + currKingdom.Color + currKingdom.Name, 10, 60, 10);
                }
                // Say "Leaving" if going from kingdom to unclaimed
                // Also need to check if kingdom exists since it might be that it was just deleted
                else if (_cache.hasKingdom(lastKingdomEnteredId)) {
                    var prevKingdom = _cache.getKingdom(lastKingdomEnteredId);
                    player.sendTitle("", "Leaving " + prevKingdom.Color + prevKingdom.Name, 10, 60, 10);
                }
            }

            _lastKingdomEntered.put(uuid, currentKingdomId);
        }
    }
}
