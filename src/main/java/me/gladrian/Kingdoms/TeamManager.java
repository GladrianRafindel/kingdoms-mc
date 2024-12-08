package me.gladrian.Kingdoms;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

public class TeamManager {
    private final Scoreboard _scoreboard;

    public TeamManager() {
        _scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void addPlayerToTeam(String playerName, Kingdom kingdom) {
        var team = getKingdomTeam(kingdom);
        if(!team.hasEntry(playerName))
            team.addEntry(playerName);
    }

    public void removePlayerFromTeam(String playerName, Kingdom kingdom) {
        var team = getKingdomTeam(kingdom);
        if(team.hasEntry(playerName))
            team.removeEntry(playerName);
    }

    public void refreshTeamPrefixes(ArrayList<Kingdom> kingdoms) {
        for (var kingdom : kingdoms) {
            updateTeamPrefix(kingdom);
        }
    }

    public void updateTeamPrefix(Kingdom kingdom) {
        var team = getKingdomTeam(kingdom);
        team.setPrefix(getTeamPrefix(kingdom));
    }

    private Team getKingdomTeam(Kingdom kingdom) {
        var teamName = getTeamName(kingdom);
        var team = _scoreboard.getTeam(teamName);
        if (team == null) {
            team = _scoreboard.registerNewTeam(teamName);
            team.setPrefix(getTeamPrefix(kingdom));
        }
        return team;
    }

    private static String getTeamPrefix(Kingdom kingdom) {
        return "§0[§r" + kingdom.Color + kingdom.ChatPrefix + "§0] ";
    }

    private static String getTeamName(Kingdom kingdom) {
        return "kingdom_" + kingdom.Id;
    }
}
