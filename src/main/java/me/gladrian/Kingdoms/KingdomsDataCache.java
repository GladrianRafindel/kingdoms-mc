package me.gladrian.Kingdoms;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class KingdomsDataCache {
    public int NextKingdomId;
    public ArrayList<Kingdom> Kingdoms;
    public HashMap<Integer, ArrayList<ClaimedChunk>> ChunksIndexedByX;
    public HashMap<UUID, KingdomMember> MembersIndexedByUuid;
    public ArrayList<Invitation> Invitations;
    public ArrayList<Message> Messages;

    public Kingdom addNewKingdom(String name, String chatPrefix, int money, ChatColor color) {
        var kingdom = new Kingdom();
        kingdom.Id = NextKingdomId++;
        kingdom.Name = name;
        kingdom.ChatPrefix = chatPrefix;
        kingdom.Money = money;
        kingdom.Color = color;
        Kingdoms.add(kingdom);
        return kingdom;
    }

    public void removeKingdom(Kingdom kingdom) {
        for (var member : kingdom.Members) {
            MembersIndexedByUuid.remove(member.PlayerUuid);
        }
        for (var chunk : kingdom.Chunks) {
            ChunksIndexedByX.get(chunk.X).remove(chunk);
        }
        Kingdoms.remove(kingdom);
    }

    public KingdomMember addNewMember(Player player, int kingdomId, KingdomMemberRank rank, String title) {
        var member = new KingdomMember();
        member.PlayerUuid = player.getUniqueId();
        member.KingdomId = kingdomId;
        member.Rank = rank;
        member.Title = title;
        member.DateJoined = new Date(System.currentTimeMillis());

        var kingdom = getKingdom(kingdomId);
        kingdom.Members.add(member);
        MembersIndexedByUuid.put(member.PlayerUuid, member);

        return member;
    }

    public void removeMember(KingdomMember member) {
        var kingdom = getKingdom(member.KingdomId);
        kingdom.Members.remove(member);
        MembersIndexedByUuid.remove(member.PlayerUuid);
    }

    public ClaimedChunk addNewChunk(int x, int y, int kingdomId) {
        var claimedChunk = new ClaimedChunk(x, y, kingdomId);
        var kingdom = getKingdom(kingdomId);
        kingdom.Chunks.add(claimedChunk);
        if (!ChunksIndexedByX.containsKey(x))
            ChunksIndexedByX.put(x, new ArrayList<>());
        ChunksIndexedByX.get(x).add(claimedChunk);
        return claimedChunk;
    }

    public void removeChunk(ClaimedChunk chunk) {
        var kingdom = getKingdom(chunk.KingdomId);
        kingdom.Chunks.remove(chunk);
        ChunksIndexedByX.get(chunk.X).remove(chunk);
    }

    public Kingdom getKingdom(int kingdomId) {
        for (var kingdom : Kingdoms) {
            if (kingdom.Id == kingdomId) {
                return kingdom;
            }
        }
        throw new IndexOutOfBoundsException("Invalid kingdom id " + kingdomId);
    }

    public @Nullable Kingdom getKingdomByPrefix(String prefix) {
        for (var kingdom : Kingdoms) {
            if (kingdom.ChatPrefix.equalsIgnoreCase(prefix)) {
                return kingdom;
            }
        }
        return null;
    }

    public KingdomMember getMember(Player player) {
        if (!hasMember(player))
            throw new IndexOutOfBoundsException("No kingdom has member with UUID " + player.getUniqueId());
        return MembersIndexedByUuid.get(player.getUniqueId());
    }

    public boolean hasKingdom(int kingdomId) {
        for (var kingdom : Kingdoms) {
            if (kingdom.Id == kingdomId) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMember(Player player) {
        return MembersIndexedByUuid.containsKey(player.getUniqueId());
    }

    public boolean hasMember(Player player, Kingdom kingdom) {
        for (var member: kingdom.Members) {
            if (member.PlayerUuid.compareTo(player.getUniqueId()) == 0) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Kingdom getCurrentClaim(int chunkX, int chunkZ) {
        if (!ChunksIndexedByX.containsKey(chunkX)) {
            return null;
        }
        for (var claimedChunk : ChunksIndexedByX.get(chunkX)) {
            if (claimedChunk.X == chunkX && claimedChunk.Z == chunkZ) {
                return getKingdom(claimedChunk.KingdomId);
            }
        }
        return null;
    }

    public ClaimedChunk getClaimedChunk(int chunkX, int chunkZ) {
        for (var claimedChunk : ChunksIndexedByX.get(chunkX)) {
            if (claimedChunk.X == chunkX && claimedChunk.Z == chunkZ) {
                return claimedChunk;
            }
        }
        throw new IndexOutOfBoundsException("Chunk " + chunkX + "," + chunkZ + " is not claimed");
    }

    public Message addNewMessage(@Nullable Player from, OfflinePlayer to, String content) {
        var message = new Message();
        message.FromUuid = from == null ? null : from.getUniqueId();
        message.ToUuid = to.getUniqueId();
        message.Content = content;
        message.DateSent = new Date(System.currentTimeMillis());
        Messages.add(message);
        return message;
    }

    public void removeMessage(Message message) {
        Messages.remove(message);
    }

    public Invitation addNewInvitation(Player invitedPlayer, int kingdomId, Player sender) {
        var invitation = new Invitation();
        invitation.InvitedPlayerUuid = invitedPlayer.getUniqueId();
        invitation.KingdomId = kingdomId;
        invitation.SentByUuid = sender.getUniqueId();
        invitation.DateSent = new Date(System.currentTimeMillis());
        Invitations.add(invitation);
        return invitation;
    }

    public void removeInvitation(Invitation invitation) {
        Invitations.remove(invitation);
    }

    public @Nullable Invitation getInvitationOrNull(Player player, int kingdomId) {
        for (var invitation : Invitations) {
            if (invitation.InvitedPlayerUuid.compareTo(player.getUniqueId()) == 0
                    && invitation.KingdomId == kingdomId) {
                return invitation;
            }
        }
        return null;
    }
}
