package me.gladrian.Kingdoms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Kingdom {
    public int Id;
    public String Name;
    public String ChatPrefix;
    public ChatColor Color;
    public float Money;
    public Location TeleportLocation;
    public ArrayList<KingdomMember> Members = new ArrayList<>();
    public ArrayList<ClaimedChunk> Chunks = new ArrayList<>();

    public static Kingdom fromJson(JSONObject kingdomObj) {
        var kingdom = new Kingdom();
        kingdom.Id = kingdomObj.getInt("id");
        kingdom.Name = kingdomObj.getString("name");
        kingdom.ChatPrefix = kingdomObj.getString("chat-prefix");
        kingdom.Money = kingdomObj.getFloat("money");
        kingdom.Color = ChatColor.getByChar(kingdomObj.getString("color").charAt(0));

        if (kingdomObj.has("teleport-location")) {
            var teleportLocationObj = kingdomObj.getJSONObject("teleport-location");
            kingdom.TeleportLocation = new Location(
                    Bukkit.getWorld("world"),
                    teleportLocationObj.getDouble("x"),
                    teleportLocationObj.getDouble("y"),
                    teleportLocationObj.getDouble("z")
            );
        }

        return kingdom;
    }

    public JSONObject toJson() {
        var kingdomJsonObj = new JSONObject();
        kingdomJsonObj.put("id", Id);
        kingdomJsonObj.put("name", Name);
        kingdomJsonObj.put("chat-prefix", ChatPrefix);
        kingdomJsonObj.put("money", Money);
        kingdomJsonObj.put("color", String.valueOf(Color.getChar()));

        if (TeleportLocation != null) {
            var teleportLocationObj = new JSONObject();
            teleportLocationObj.put("x", TeleportLocation.getX());
            teleportLocationObj.put("y", TeleportLocation.getY());
            teleportLocationObj.put("z", TeleportLocation.getZ());
            kingdomJsonObj.put("teleport-location", teleportLocationObj);
        }

        return kingdomJsonObj;
    }

    public ArrayList<Player> getOnlineMembers() {
        var onlineMembers = new ArrayList<Player>();
        for (var member : Members) {
            var player = Bukkit.getPlayer(member.PlayerUuid);
            if (player != null && player.isOnline()) {
                onlineMembers.add(player);
            }
        }
        return onlineMembers;
    }
}
