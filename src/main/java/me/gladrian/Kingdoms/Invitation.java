package me.gladrian.Kingdoms;

import org.json.JSONObject;
import java.util.Date;
import java.util.UUID;

public class Invitation {
    public int KingdomId;
    public UUID InvitedPlayerUuid;
    public Date DateSent;
    public UUID SentByUuid;

    public static Invitation fromJson(JSONObject invitationJsonObj) {
        var invitation = new Invitation();
        invitation.KingdomId = invitationJsonObj.getInt("kingdom-id");
        invitation.InvitedPlayerUuid = UUID.fromString(invitationJsonObj.getString("invited-player-uuid"));
        invitation.SentByUuid = UUID.fromString(invitationJsonObj.getString("sent-by-uuid"));
        invitation.DateSent = new Date(invitationJsonObj.getLong("date-sent"));
        return invitation;
    }

    public JSONObject toJson() {
        var invitationJsonObj = new JSONObject();
        invitationJsonObj.put("kingdom-id", KingdomId);
        invitationJsonObj.put("invited-player-uuid", InvitedPlayerUuid.toString());
        invitationJsonObj.put("sent-by-uuid", SentByUuid.toString());
        invitationJsonObj.put("date-sent", DateSent.getTime());
        return invitationJsonObj;
    }
}
