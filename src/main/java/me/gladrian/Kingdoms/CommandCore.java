package me.gladrian.Kingdoms;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerProfile;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandCore implements CommandExecutor {
    private Config _config;
    private KingdomsDAL _dal;
    private KingdomsDataCache _cache;
    private TeamManager _teamManager;

    public CommandCore(Config config, KingdomsDAL dal, KingdomsDataCache cache, TeamManager teamManager) {
        _config = config;
        _dal = dal;
        _cache = cache;
        _teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getConsoleSender().sendMessage("error: /k can only be used by a player");
            return false;
        }
        Player player = (Player) sender;

        String subCommand = (args.length >= 1) ? args[0].toLowerCase() : "";
        switch (subCommand) {
            case "": sendHelpMenu(player, null); break;
            case "help": sendHelpMenu(player, args); break;
            case "info": sendInfo(player, args); break;
            case "create": createNewKingdom(player, args); break;
            case "claim": claimChunk(player); break;
            case "unclaim": unclaimChunk(player); break;
            case "set": setKingdomProperty(player, args); break;
            case "tp": teleportToKingdom(player); break;
            case "deposit": depositMoney(player, args); break;
            case "list": listKingdoms(player, args); break;
            case "invite": invitePlayer(player, args); break;
            case "uninvite": uninvitePlayer(player, args); break;
            case "invited": listInvitedPlayers(player); break;
            case "remove": removePlayerFromKingdom(player, args); break;
            case "invitations": viewMyInvitations(player); break;
            case "accept": acceptInvitation(player, args); break;
            case "reject": rejectInvitation(player, args); break;
            case "leave": leaveKingdom(player, args); break;
            case "delete": deleteKingdom(player, args); break;
            case "messages": viewMyMessages(player); break;
            case "backup": adhocBackup(player); break;

            default:
                player.sendMessage(ChatColor.RED + "No such Kingdoms command '" + subCommand + "'.");
                return false;
        }
        return true;
    }

    private void sendHelpMenu(Player player, @Nullable String[] args) {
        var helpInfo = new ArrayList<String>();
        helpInfo.add("/k info §7- Display info about your kingdom. Use '/k info [<kingdom prefix>]' " +
                "for information about another kingdom.");
        helpInfo.add("/k create [<name>] [<chat prefix>] §7- Create a new kingdom. " +
                "To set a name with spaces in it, use quotes (\") around the whole name. " +
                "Chat prefix may only be " + _config.ChatPrefixCharacterLimit + " characters long.");
        helpInfo.add("/k list §7- List information about every kingdom.");
        helpInfo.add("/k tp §7- Teleport to your kingdom.");
        helpInfo.add("/k messages §7- View your unread messages.");
        helpInfo.add("/k claim §7- Claim the chunk you're standing in for your kingdom.");
        helpInfo.add("/k unclaim §7- Unclaim the chunk you're standing in.");
        helpInfo.add("/k set [name | chat-prefix | tp | color] [<value>] §7- Change a setting for your kingdom.");
        helpInfo.add("/k invite [<username>] §7- Invite a player to join your kingdom.");
        helpInfo.add("/k uninvite [<username>] §7- Revoke an invitation for a player to join your kingdom.");
        helpInfo.add("/k invited §7- List all players you have been invited to join your kingdom.");
        helpInfo.add("/k remove [<username>] §7- Remove a player from your kingdom.");
        helpInfo.add("/k deposit [<number of diamonds>] §7- Convert 1 diamond to " +
                _config.DiamondToMoneyRate + " " + _config.MoneyNamePlural +
                " and deposit into your kingdom's treasury.");
        helpInfo.add("/k invitations §7- See which kingdoms have invited you to become a member .");
        helpInfo.add("/k accept [<prefix of kingdom>] §7- Accept a request to join a kingdom.");
        helpInfo.add("/k reject [<prefix of kingdom>] §7- Reject a request to join a kingdom.");
        helpInfo.add("/k leave §7- Leave your kingdom.");
        helpInfo.add("/k delete §7- Delete your kingdom. Only the leader of a kingdom can do this.");

        int numPerPage = 5;
        int numPages = (int)Math.ceil((double)helpInfo.size() / (double)numPerPage);

        int selectedPageIndex = 0;
        try {
            int argPageIndex = Integer.parseUnsignedInt(args[1]) - 1;
            selectedPageIndex = argPageIndex < numPages ? argPageIndex : 0;
        }
        catch (Exception e) { }

        player.sendMessage("§6============== Kingdoms Help Menu ==============");
        for (int i = 0; i < numPerPage; i++) {
            int index = selectedPageIndex * numPerPage + i;
            if (index < helpInfo.size())
                player.sendMessage(helpInfo.get(index));
        }
        player.sendMessage("§6=== §ePage §f" + (selectedPageIndex + 1) + "/" + numPages + "§e. Use §6/k help [<page #>] §eto go to another page.");
    }

    private void listKingdoms(Player player, String[] args) {
        int numPerPage = 5;
        int selectedPageIndex = 0;
        int numPages = (int)Math.ceil((double)_cache.Kingdoms.size() / (double)numPerPage);
        try {
            int argPageIndex = Integer.parseUnsignedInt(args[1]) - 1;
            selectedPageIndex = argPageIndex < numPages ? argPageIndex : 0;
        }
        catch (Exception e) { }

        player.sendMessage("§6============== Kingdom List ==============");
        for (int i = 0; i < numPerPage; i++) {
            int index = selectedPageIndex * numPerPage + i;
            if (index < _cache.Kingdoms.size()) {
                var kingdom = _cache.Kingdoms.get(index);
                player.sendMessage((index + 1) + ". " + kingdom.Name + " (Prefix: " + kingdom.ChatPrefix + ")");
            }
        }
        player.sendMessage("§6=== §ePage §f" + (selectedPageIndex + 1) + "/" + numPages + "§e. Use §6/k list [<page #>] §eto go to another page.");
        player.sendMessage("§6=== §eUse §7/k info [<kingdom prefix>] §eto see more information about a kingdom");
    }

    private void sendInfo(Player player, String[] args) {
        @Nullable Kingdom selectedKingdom = null;
        // Lookup by prefix if not just '/k info' (prefix is second argument)
        if (args.length > 1) {
            for (var kingdom : _cache.Kingdoms) {
                if (kingdom.ChatPrefix.equalsIgnoreCase(args[1])) {
                    selectedKingdom = kingdom;
                    break;
                }
            }
            if (selectedKingdom == null) {
                player.sendMessage("§cNo kingdom with the prefix '" + args[2] + "' exists.");
                return;
            }
        }
        // If just '/k info', get the player's kingdom
        else if (_cache.hasMember(player)) {
            var kingdomId = _cache.getMember(player).KingdomId;
            selectedKingdom = _cache.getKingdom(kingdomId);
        }
        else {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }

        boolean isMemberOfSelected = _cache.hasMember(player)
                && _cache.getMember(player).KingdomId == selectedKingdom.Id;

        var leaderUsername = "";
        var memberUsernames = new ArrayList<String>();
        for (var member : selectedKingdom.Members) {
            var p = Bukkit.getOfflinePlayer(member.PlayerUuid);
            var name = p.getName();
            if (member.Rank == KingdomMemberRank.Leader) {
                leaderUsername = name;
            } else {
                memberUsernames.add(name);
            }
        }

        player.sendMessage("§6============== Kingdom Info ==============");
        player.sendMessage("§7Name: §f" + selectedKingdom.Name);
        player.sendMessage("§7Prefix: §f" + selectedKingdom.Color + selectedKingdom.ChatPrefix);
        player.sendMessage("§7Leader: §f" + leaderUsername);
        player.sendMessage("§7Citizens: §f" + String.join(", ", memberUsernames));
        if (isMemberOfSelected) {
            player.sendMessage("§7Treasury: §f" + (int)Math.floor(selectedKingdom.Money) + " " + _config.MoneyNamePlural);
            player.sendMessage("§7Claimed: §f" + selectedKingdom.Chunks.size() + " chunks");
        }
    }

    private void createNewKingdom(Player player, String[] args) {
        if (_cache.hasMember(player)) {
            player.sendMessage("§cYou are already a member of a kingdom. To leave it, use /k leave.");
            return;
        }

        String cmdUsage = "/k create [<name>] [<chat prefix>]";
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Kingdom name and chat prefix are required: " + cmdUsage);
            return;
        }
        String name = "";
        String chatPrefix = "";

        // Allow spaces in name if it's surrounded by quotes
        var nameLastArgIndex = new AtomicInteger();
        name = getQuotedString(args, 1, nameLastArgIndex);

        // Allow spaces in prefix as well
        int prefixStartIndex = nameLastArgIndex.intValue() + 1;
        if (prefixStartIndex + 1 > args.length) {
            player.sendMessage(ChatColor.RED + "A chat prefix for members of the kingdom '" + name + "' is required: " + cmdUsage);
            return;
        }
        chatPrefix = args[prefixStartIndex];
        if (chatPrefix.length() > _config.ChatPrefixCharacterLimit) {
            player.sendMessage("§cChat prefix cannot be longer than " + _config.ChatPrefixCharacterLimit + " characters.");
            return;
        }

        // Kingdom cannot have same name or prefix as another kingdom
        for (var otherKingdom : _cache.Kingdoms) {
            if (otherKingdom.Name.equalsIgnoreCase(name)) {
                player.sendMessage("§cAnother kingdom already has the name '" + name + "'.");
                return;
            }
            if (otherKingdom.ChatPrefix.equalsIgnoreCase(chatPrefix)) {
                player.sendMessage("§cAnother kingdom already has the chat prefix [" + chatPrefix + "].");
                return;
            }
        }

        var kingdom = _cache.addNewKingdom(name, chatPrefix, _config.InitialMoneyForNewKingdom, ChatColor.WHITE);

        var leaderTitle = _config.SovereignTitleOptions != null && _config.SovereignTitleOptions.size() > 0
                ? _config.SovereignTitleOptions.get(0) : "";
        var member = _cache.addNewMember(player, kingdom.Id, KingdomMemberRank.Leader, leaderTitle);
        _teamManager.addPlayerToTeam(player.getName(), kingdom);

        _dal.saveChunksAsync(kingdom);
        _dal.saveMembersAsync(kingdom);
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);

        player.sendMessage("§eYou are now the leader of a new kingdom named '§f" + kingdom.Name
            + "§e'! Members will have the chat prefix " + kingdom.Color + kingdom.ChatPrefix + "§e.");
        player.sendMessage("Claim your first chunk using /k claim.");
    }

    private void claimChunk(Player player) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        int kingdomId = _cache.getMember(player).KingdomId;
        var kingdom = _cache.getKingdom(kingdomId);
        var playerLocation = player.getLocation();
        int x = playerLocation.getChunk().getX();
        int z = playerLocation.getChunk().getZ();

        if (playerLocation.getWorld() != Bukkit.getWorld("world")) {
            player.sendMessage("§cYou can only claim chunks in the overworld.");
            return;
        }

        var kingdomWithClaimOnChunk = _cache.getCurrentClaim(x, z);
        if (kingdomWithClaimOnChunk != null) {
            player.sendMessage("§cThis chunk is already claimed by " + kingdomWithClaimOnChunk.Name + ".");
            return;
        }

        if (kingdom.Chunks.size() > 0) {
            var adjacentClaims = new ArrayList<Kingdom>();
            adjacentClaims.add(_cache.getCurrentClaim(x - 1, z));
            adjacentClaims.add(_cache.getCurrentClaim(x + 1, z));
            adjacentClaims.add(_cache.getCurrentClaim(x, z - 1));
            adjacentClaims.add(_cache.getCurrentClaim(x, z + 1));
            if (!adjacentClaims.contains(kingdom)) {
                player.sendMessage("§cThis chunk is not adjacent to another of your kingdom's claimed chunks.");
                return;
            }
        }

        if (kingdom.TeleportLocation != null
                && kingdom.TeleportLocation.getChunk().getX() == x
                && kingdom.TeleportLocation.getChunk().getZ() == z) {
            player.sendMessage("§cThe teleport location for your kingdom is in this chunk. Please move the teleport location before unclaiming.");
            return;
        }

        if (kingdom.Money < _config.ChunkClaimCost) {
            player.sendMessage("§cYour kingdom does have enough " + _config.MoneyNamePlural
                    + " to claim another chunk. " + kingdom.Name + " has "
                    + kingdom.Money + " " + _config.MoneyNamePlural + ". "
                    + "Claiming a chunk costs " + _config.ChunkClaimCost + " " + _config.MoneyNamePlural + ".");
            return;
        }

        _cache.addNewChunk(x, z, kingdomId);
        kingdom.Money -= _config.ChunkClaimCost;
        player.sendMessage("§eClaimed chunk " + x + "," + z + " for "
                + _config.ChunkClaimCost + " " + _config.MoneyNamePlural + ".");

        if (kingdom.Chunks.size() == 1) {
            kingdom.TeleportLocation = playerLocation;
            player.sendMessage("§eYou claimed the first chunk for your kingdom!");
            player.sendMessage("§eYour location has been set to the teleport location for "
                    + kingdom.Name + ". To change it, use §f/k set tp§e within a claimed chunk.");
        }

        _dal.saveChunksAsync(kingdom);
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
    }

    private void unclaimChunk(Player player) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        int kingdomId = _cache.getMember(player).KingdomId;
        var kingdom = _cache.getKingdom(kingdomId);
        var playerLocation = player.getLocation();
        int x = playerLocation.getChunk().getX();
        int z = playerLocation.getChunk().getZ();

        if (playerLocation.getWorld() != Bukkit.getWorld("world")) {
            player.sendMessage("§cYou can only unclaim chunks in the overworld.");
            return;
        }

        var kingdomWithClaimOnChunk = _cache.getCurrentClaim(x, z);
        if (kingdomWithClaimOnChunk != kingdom) {
            player.sendMessage("§cThis chunk is not claimed by your kingdom.");
            return;
        }
        var claimedChunk = _cache.getClaimedChunk(x, z);

        if (kingdom.TeleportLocation != null
                && kingdom.TeleportLocation.getChunk().getX() == x
                && kingdom.TeleportLocation.getChunk().getZ() == z) {
            player.sendMessage("§cThe teleport location for your kingdom is in this chunk. Please move the teleport location before unclaiming.");
            return;
        }

        if (kingdom.Chunks.size() < 2) {
            player.sendMessage("§cYou cannot unclaim the only chunk your kingdom has claimed.");
            return;
        }

        _cache.removeChunk(claimedChunk);
        kingdom.Money += _config.ChunkClaimCost;
        player.sendMessage("§eUnclaimed chunk " + x + "," + z + ". Your kingdom now has "
                + _config.ChunkClaimCost + " additional " + _config.MoneyNamePlural + ".");

        _dal.saveChunksAsync(kingdom);
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
    }

    private void setKingdomProperty(Player player, String[] args) {
        var cmdUsage = "§cUsage: /k set [tp | name | prefix | color]";
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (member.Rank != KingdomMemberRank.Leader) {
            player.sendMessage("§cOnly the leader of a kingdom can use /k set.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(cmdUsage);
            return;
        }

        var propertyName = args[1].toLowerCase();
        switch (propertyName) {
            case "tp" -> setTeleportLocation(player, kingdom);
            case "name" -> setName(player, kingdom, args);
            case "prefix" -> setChatPrefix(player, kingdom, args);
            case "color" -> setColor(player, kingdom, args);
            default -> player.sendMessage("§cUnrecognized property '" + propertyName + "'. " + cmdUsage);
        }
    }

    private void setColor(Player player, Kingdom kingdom, String[] args) {
        var colorOptions = Arrays.stream(org.bukkit.ChatColor.values())
                .filter(c -> c.getChar() <= 'f').toList();
        var stringAvailableColors = "Available colors: " + String.join(" ",
                colorOptions.stream().map(c -> "§" + c.getChar() + c.getChar()).toList());
        if (args.length < 3) {
            player.sendMessage("§cUsage: /k set color [<color code>]");
            player.sendMessage(stringAvailableColors);
            return;
        }

        var selectedColorCode = args[2].charAt(0);
        if (colorOptions.stream().noneMatch(c -> c.getChar() == selectedColorCode)) {
            player.sendMessage("§c'" + selectedColorCode + "' is not a valid color code.");
            player.sendMessage(stringAvailableColors);
            return;
        }

        kingdom.Color = ChatColor.getByChar(selectedColorCode);
        _teamManager.updateTeamPrefix(kingdom);
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        player.sendMessage("§eYour kingdom's color has been set to " + kingdom.Color + "this color§e.");
    }

    private void setTeleportLocation(Player player, Kingdom kingdom) {
        var location = player.getLocation();
        var chunk = location.getChunk();
        var kingdomWithClaimOnChunk = _cache.getCurrentClaim(chunk.getX(), chunk.getZ());
        if (kingdomWithClaimOnChunk != kingdom) {
            player.sendMessage("§cThe teleport location for your kingdom must be within one of its claimed chunks.");
            return;
        }
        kingdom.TeleportLocation = location;
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        player.sendMessage("§eTeleport location for " + kingdom.Name + " set to your current position.");
    }

    private void setName(Player player, Kingdom kingdom, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /k set name [<new name>]");
            return;
        }
        var newName = getQuotedString(args, 2, null);
        for (var otherKingdom : _cache.Kingdoms) {
            if (otherKingdom.Name.equalsIgnoreCase(newName)) {
                player.sendMessage("§cAnother kingdom already has the name '" + newName + "'.");
                return;
            }
        }
        kingdom.Name = newName;
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        player.sendMessage("§eThe name of your kingdom has been changed to '" + newName + "'.");
    }

    private void setChatPrefix(Player player, Kingdom kingdom, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /k set prefix [<new name>]");
            return;
        }
        var newPrefix = args[2];
        if (newPrefix.length() > _config.ChatPrefixCharacterLimit) {
            player.sendMessage("§cChat prefix cannot be longer than " + _config.ChatPrefixCharacterLimit + " characters.");
            return;
        }
        for (var otherKingdom : _cache.Kingdoms) {
            if (otherKingdom.ChatPrefix.equalsIgnoreCase(newPrefix)) {
                player.sendMessage("§cAnother kingdom already has the chat prefix [" + newPrefix + "].");
                return;
            }
        }
        kingdom.ChatPrefix = newPrefix;
        _teamManager.updateTeamPrefix(kingdom);
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        player.sendMessage("§eThe chat prefix for your kingdom members has been changed to "
                + kingdom.Color + newPrefix + "§e.");
    }

    private void teleportToKingdom(Player player) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (kingdom.TeleportLocation == null) {
            player.sendMessage("§cNo teleport location has been set for your kingdom. Use '/k set tp' to set it.");
            return;
        }
        player.teleport(kingdom.TeleportLocation);
        player.playSound(kingdom.TeleportLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        player.sendMessage("§eTeleported to " + kingdom.Color + kingdom.Name + "§e.");
    }

    private void depositMoney(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var numDiamonds = 0;
        try {
            numDiamonds = Integer.parseUnsignedInt(args[1]);
        }
        catch (Exception e) {
            player.sendMessage("§cUsage: /k deposit [<number of diamonds>]");
            return;
        }
        if (!player.getInventory().contains(Material.DIAMOND, numDiamonds)) {
            player.sendMessage("§cYou do not have " + numDiamonds + " diamonds.");
            return;
        }

        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, numDiamonds));
        var moneyAmount = numDiamonds * _config.DiamondToMoneyRate;
        kingdom.Money += moneyAmount;
        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        player.sendMessage("§eDeposited §6" + moneyAmount + " " + _config.MoneyNamePlural + " for " + numDiamonds + " diamonds.");
    }

    private void invitePlayer(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /k invite [<player username>]");
            return;
        }

        var inviteUsername = args[1];
        var invitePlayer = Bukkit.getPlayer(inviteUsername);
        if (invitePlayer == null) {
            player.sendMessage("§cNo player with the username '" + inviteUsername + "' could be found");
            return;
        }

        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (_cache.hasMember(invitePlayer, kingdom)) {
            player.sendMessage("§c" + invitePlayer.getName() + " is already a member of your kingdom.");
            return;
        }
        if (_cache.getInvitationOrNull(invitePlayer, kingdom.Id) != null) {
            player.sendMessage(invitePlayer.getName() + " has already been invited to join your kingdom.");
            return;
        }
        _cache.addNewInvitation(invitePlayer, kingdom.Id, player);
        _dal.saveInvitationsAsync(_cache.Invitations);
        queueMessageToPlayer(invitePlayer,
                "You have been invited to join " + kingdom.Name +
                ". Use §b/k accept " + kingdom.ChatPrefix + " §f to accept or " +
                "§7/k reject " + kingdom.ChatPrefix + " §f to reject the invitation.",
                player);
        player.sendMessage("§eYou have invited §6" + invitePlayer.getName() +
                " §eto join §f" + kingdom.Name + "§e.");
    }

    private void uninvitePlayer(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /k uninvite [<player username>]");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);

        var uninviteUsername = args[1];
        var uninvitePlayer = Bukkit.getPlayer(uninviteUsername);
        if (uninvitePlayer == null || _cache.getInvitationOrNull(uninvitePlayer, kingdom.Id) == null) {
            player.sendMessage("§cNo invitation has been sent to " + uninviteUsername + ".");
            return;
        }

        var invitation = _cache.getInvitationOrNull(uninvitePlayer, kingdom.Id);
        _cache.removeInvitation(invitation);
        _dal.saveInvitationsAsync(_cache.Invitations);
        queueMessageToPlayer(uninvitePlayer,
                "Your invitation to join " + kingdom.Name + " has been §crevoked§f.",
                player);
        player.sendMessage("§eYou have §crevoked §ethe invitation for §6" + uninvitePlayer.getName() +
                " §eto join §f" + kingdom.Name + "§e.");
    }

    private void listInvitedPlayers(Player player) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);

        if (_cache.Invitations.size() < 1) {
            player.sendMessage("There are no players currently invited to join your kingdom.");
            return;
        }

        player.sendMessage("§6===== Players invited to join " + kingdom.Name + " =====");
        var count = 0;
        for (var invitation : _cache.Invitations.stream().filter(i -> i.KingdomId == kingdom.Id).toList()) {
            var invitedPlayer = Bukkit.getOfflinePlayer(invitation.InvitedPlayerUuid);
            var sentByPlayer = Bukkit.getOfflinePlayer(invitation.SentByUuid);
            player.sendMessage(++count + ". §6" + invitedPlayer.getName() + " §7invited by "
                    + sentByPlayer.getName() + " on §f" + invitation.DateSent.toString() + "§7");
        }
    }

    private void removePlayerFromKingdom(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /k remove [<player username>]");
            return;
        }

        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (member.Rank != KingdomMemberRank.Leader) {
            player.sendMessage("§cOnly the leader of a kingdom can remove its members.");
            return;
        }

        var removeUsername = args[1];
        var removePlayer = Bukkit.getPlayer(removeUsername);
        if (removePlayer == null || !_cache.hasMember(removePlayer, kingdom)) {
            player.sendMessage("§c" + removeUsername + " is no a member of your kingdom.");
            return;
        }
        if (removePlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
            player.sendMessage("§cYou cannot remove yourself. Use '/k delete' to delete your kingdom.");
            return;
        }

        var memberToRemove = _cache.getMember(removePlayer);
        _cache.removeMember(memberToRemove);
        _teamManager.removePlayerFromTeam(removePlayer.getName(), kingdom);
        _dal.saveMembersAsync(kingdom);
        player.sendMessage("§eYou have removed §f" + removeUsername + " §efrom your kingdom.");
        queueMessageToPlayer(removePlayer,
                player.getName() + " §ehas §cremoved §eyou from " + kingdom.Color + kingdom.Name + "§e.", null);
        for (var otherMember : kingdom.Members.stream().filter(m ->
                m.PlayerUuid != player.getUniqueId()).toList()) {
            var p = Bukkit.getPlayer(otherMember.PlayerUuid);
            queueMessageToPlayer(p,removeUsername + " §ehas been §cremoved §efrom your kingdom by §7"
                    + player.getName() + "§e.", null);
        }
    }

    private void viewMyInvitations(Player player) {
        var invitations = _cache.Invitations.stream().filter(i -> i.InvitedPlayerUuid.compareTo(player.getUniqueId()) == 0).toList();
        if (invitations.size() < 1) {
            player.sendMessage("You have not been invited to join any kingdoms.");
            return;
        }

        player.sendMessage("§6============== Your invitations ==============");
        for (var invitation : invitations) {
            var invitedByName = Bukkit.getOfflinePlayer(invitation.SentByUuid).getName();
            var kingdom = _cache.getKingdom(invitation.KingdomId);
            player.sendMessage(invitedByName + " has invited your to join " + kingdom.Color + kingdom.Name);
        }
    }

    private void acceptInvitation(Player player, String[] args) {
        if (_cache.hasMember(player)) {
            player.sendMessage("§cYou must leave your current kingdom before joining another kingdom.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /k accept [<kingdom prefix>]");
            return;
        }

        var prefix = args[1];
        var kingdom = _cache.getKingdomByPrefix(prefix);
        if (kingdom == null) {
            player.sendMessage("§cThere is no kingdom with the prefix '" + prefix + "'. " +
                    "Use '/k list' to list all valid prefixes.");
            return;
        }

        var invitation = _cache.getInvitationOrNull(player, kingdom.Id);
        if (invitation == null) {
            player.sendMessage("§cYou have not been invited to join " + kingdom.Name + ". " +
                    "Use '/k invitations' to view the kingdoms that have invited you to join.");
            return;
        }

        _cache.addNewMember(player, kingdom.Id, KingdomMemberRank.Citizen, "");
        _teamManager.addPlayerToTeam(player.getName(), kingdom);
        _dal.saveMembersAsync(kingdom);
        _cache.removeInvitation(invitation);
        _dal.saveInvitationsAsync(_cache.Invitations);

        player.sendMessage("§eYou have joined " + kingdom.Color + kingdom.Name + "§e! " +
                "Use §f/k help §efor information about kingdom commands.");
        for (var otherMember : kingdom.Members.stream().filter(m ->
                m.PlayerUuid != player.getUniqueId()).toList()) {
            var p = Bukkit.getPlayer(otherMember.PlayerUuid);
            queueMessageToPlayer(p,player.getName() + " §ehas joined your kingdom!", null);
        }
    }

    private void rejectInvitation(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /k reject [<kingdom prefix>]");
            return;
        }

        var prefix = args[1];
        var kingdom = _cache.getKingdomByPrefix(prefix);
        if (kingdom == null) {
            player.sendMessage("§cThere is no kingdom with the prefix '" + prefix + "'. " +
                    "Use '/k list' to list all valid prefixes.");
            return;
        }

        var invitation = _cache.getInvitationOrNull(player, kingdom.Id);
        if (invitation == null) {
            player.sendMessage("§cYou have not been invited to join " + kingdom.Name + ". " +
                    "Use '/k invitations' to view the kingdoms that have invited you to join.");
            return;
        }

        _cache.removeInvitation(invitation);
        _dal.saveInvitationsAsync(_cache.Invitations);

        player.sendMessage("§eYou have §crejected §ethe invitation to join "
                + kingdom.Color + kingdom.Name + "§e.");
        var invitingPlayer = Bukkit.getPlayer(invitation.SentByUuid);
        queueMessageToPlayer(invitingPlayer, player.getName()
                + " has §crejected §fyour invitation to join your kingdom.", player);
    }

    private void leaveKingdom(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (member.Rank == KingdomMemberRank.Leader) {
            player.sendMessage("§cYou cannot leave your kingdom since you are its leader. " +
                    "Use '/k delete' if you wish to delete your kingdom.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage("§eAre you sure that you want to leave " + kingdom.Color + kingdom.Name
                    + "§e? Use §f/k leave confirm §eto confirm.");
            return;
        }

        _cache.removeMember(member);
        _teamManager.removePlayerFromTeam(player.getName(), kingdom);
        _dal.saveMembersAsync(kingdom);

        player.sendMessage("§eYou have §cleft §ethe kingdom " + kingdom.Color + kingdom.Name + "§e.");
        for (var otherMember : kingdom.Members) {
            var p = Bukkit.getPlayer(otherMember.PlayerUuid);
            queueMessageToPlayer(p,player.getName() + " has §cleft §fyour kingdom.", null);
        }
    }

    private void deleteKingdom(Player player, String[] args) {
        if (!_cache.hasMember(player)) {
            player.sendMessage("§cYou are not a member of a kingdom.");
            return;
        }
        var member = _cache.getMember(player);
        var kingdom = _cache.getKingdom(member.KingdomId);
        if (member.Rank != KingdomMemberRank.Leader) {
            player.sendMessage("§cOnly the leader of a kingdom can delete it.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage("§eAre you sure that you want to delete " + kingdom.Color + kingdom.Name
                    + "§e? All of your kingdom's progress with be §cpermanently §edeleted. "
                    + "Use §f/k delete confirm §eto confirm.");
            return;
        }

        var otherPlayersInKingdom = new ArrayList<OfflinePlayer>();
        for (var otherMember : kingdom.Members.stream().filter(m ->
                m.PlayerUuid.compareTo(player.getUniqueId()) != 0).toList()) {
            otherPlayersInKingdom.add(Bukkit.getOfflinePlayer(otherMember.PlayerUuid));
        }

        _cache.removeKingdom(kingdom);
        _teamManager.removePlayerFromTeam(player.getName(), kingdom);

        _dal.saveAllKingdomInfoAsync(_cache.Kingdoms);
        _dal.deleteChunks(kingdom);
        _dal.deleteMembers(kingdom);

        player.sendMessage("§eYou have §cdeleted §ethe kingdom " + kingdom.Color + kingdom.Name + "§e.");
        for (var p : otherPlayersInKingdom) {
            _teamManager.removePlayerFromTeam(p.getName(), kingdom);
            queueMessageToPlayer(p,player.getName() + " has §cdeleted §fthe kingdom "
                    + kingdom.Color + kingdom.Name + "§f. You are no longer a member of a kingdom.", null);
        }
    }

    private void viewMyMessages(Player player) {
        var messages = _cache.Messages.stream()
                .filter(m -> m.ToUuid.compareTo(player.getUniqueId()) == 0)
                .sorted().toList();
        if (messages.size() < 1) {
            player.sendMessage("You have no unread messages.");
            return;
        }
        //Collections.sort(messages);
        var start = Math.max(messages.size() - 3, 0);
        var end = messages.size();
        var latestMessages = messages.subList(start, end);

        player.sendMessage("§6============== Kingdom Messages ==============");
        for (var message : latestMessages.stream().sorted().toList()) {
            var sent = (message.FromUuid == null ? "" : "From " + Bukkit.getOfflinePlayer(message.FromUuid).getName() + " ")
                    + message.DateSent.toString();
            player.sendMessage("§7" + sent + "> §r" + message.Content);
            _cache.removeMessage(message);
        }
        if (latestMessages.size() < messages.size()) {
            player.sendMessage("§6=== §eYou have more unread messages. Enter §f/k messages §eagain to read them.");
        }
        _dal.saveMessagesAsync(_cache.Messages);
    }

    private void queueMessageToPlayer(OfflinePlayer to, String content, @Nullable Player from) {
        _cache.addNewMessage(from, to, content);
        _dal.saveMessagesAsync(_cache.Messages);
        var toPlayer = Bukkit.getPlayer(to.getUniqueId());
        if (toPlayer != null && toPlayer.isOnline()) {
            toPlayer.sendMessage("§eYou have a new message" +
                    (from == null ? "." : " from §f" + from.getDisplayName() + "§e.") +
                    " §eUse §6/k messages §eto read it.");
        }
    }

    private void adhocBackup(Player player) {
        if (!player.isOp()) {
            player.sendMessage("§cYou do not have permission to perform this command.");
        }
        else {
            _dal.backupData();
            player.sendMessage("§eSuccessfully triggered backup of Kingdoms data.");
        }
    }

    /* getQuotedString
    Description: Given an array of strings, returns the first consecutive string. This can be either:
        1) a single string with no whitespace, or
        2) a string with whitespace that is surrounded by quotes
    Parameters:
        * strings - array of strings
        * startIndex - the index in the array to start at
        * returnEndIndex - the index that the consecutive string ends at, returned by updating the variable passed in
    Examples:
        * getQuotedString([hello,bye], 0, endIndex) => returns 'hello', endIndex = 0
        * getQuotedString([nothing, "hello, there!", bye], 1, endIndex) => returns 'hello there!', endIndex = 2
     */
    private String getQuotedString(String[] strings, int startIndex, @Nullable AtomicInteger returnEndIndex) {
        String str = "";
        int endIndex = startIndex;
        if (strings[startIndex].charAt(0) == '"') {
            while (endIndex < strings.length) {
                if (strings[endIndex].charAt(strings[endIndex].length() - 1) == '"') {
                    str += " " + strings[endIndex].substring(0, strings[endIndex].length() - 1);
                    break;
                } else {
                    str += " " + strings[endIndex];
                }
                ++endIndex;
            }
            str =  str.replace("\"", "");
            str = str.trim();
        } else {
            str = strings[startIndex];
        }
        if (returnEndIndex != null) returnEndIndex.set(endIndex);
        return str;
    }
}