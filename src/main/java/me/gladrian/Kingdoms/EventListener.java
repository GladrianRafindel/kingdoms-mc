package me.gladrian.Kingdoms;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;

public class EventListener implements Listener {
    private Config _config;
    private KingdomsDAL _dal;
    private KingdomsDataCache _cache;
    private TeamManager _teamManager;

    public EventListener(Config config, KingdomsDAL dal, KingdomsDataCache cache, TeamManager teamManager) {
        _config = config;
        _dal = dal;
        _cache = cache;
        _teamManager = teamManager;
    }

    private boolean locationIsInKingdom(Location location) {
        if (!location.getWorld().getName().equalsIgnoreCase("world")) return false;
        var chunk = location.getChunk();
        var kingdom = _cache.getCurrentClaim(chunk.getX(), chunk.getZ());
        return kingdom != null;
    }

    private boolean playerCanBuild(Player player, Block block) {
        if (!block.getWorld().getName().equalsIgnoreCase("world")) return true;
        var chunk = block.getChunk();
        var kingdom = _cache.getCurrentClaim(chunk.getX(), chunk.getZ());
        if (kingdom == null) return true;
        return _cache.hasMember(player, kingdom);
    }

    //region <PLAYER EVENTS>
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        // Set player's prefix if in kingdom
        if (_cache.hasMember(player)) {
            var kingdomId = _cache.getMember(player).KingdomId;
            var kingdom = _cache.getKingdom(kingdomId);
            _teamManager.addPlayerToTeam(player.getName(), kingdom);
        }

        // Send unread messages notification
        var messages = _cache.Messages.stream().filter(m -> m.ToUuid.compareTo(player.getUniqueId()) == 0).toList();
        if (messages.size() > 0) {
            player.sendMessage("§6[Kingdoms] §eYou have §f" + messages.size() + "§e unread message"
                    + (messages.size() > 1 ? "s" : "") + ". Use §f/k messages §eto read them.");
        }
    }

    @EventHandler
    public void onOpenContainer(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        var block = event.getClickedBlock();
        if (!(block.getState() instanceof InventoryHolder)) return;

        var player = event.getPlayer();
        if (!playerCanBuild(player, block)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot open this block's inventory. You are not a member of the " +
                    "kingdom that has claimed this chunk.");
        }
    }
    //endregion

    //region <BLOCK EVENTS>
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        if (!playerCanBuild(player, event.getBlock())) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks here. You are not a member of the " +
                    "kingdom that has claimed this chunk.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        var player = event.getPlayer();
        if (!playerCanBuild(player, event.getBlock())) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks here. You are not a member of the " +
                    "kingdom that has claimed this chunk.");
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (locationIsInKingdom(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (locationIsInKingdom(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
    //endregion
}
