package me.gladrian.Kingdoms;
import org.json.JSONObject;
import java.util.Date;
import java.util.UUID;

public class KingdomMember {
    public UUID PlayerUuid;
    public int KingdomId;
    public KingdomMemberRank Rank = KingdomMemberRank.Citizen;
    public String Title = "";
    public Date DateJoined = new Date();

    public static KingdomMember fromJson(JSONObject memberObj, int kingdomId) {
        var member = new KingdomMember();
        member.PlayerUuid = UUID.fromString(memberObj.getString("uuid"));
        member.KingdomId = kingdomId;
        member.Rank = KingdomMemberRank.valueOf(memberObj.getString("rank"));
        if (memberObj.has("title"))
            member.Title = memberObj.getString("title");
        member.DateJoined = new Date(memberObj.getLong("date-join-ms"));
        return member;
    }

    public JSONObject toJson() {
        var memberJsonObj = new JSONObject();
        memberJsonObj.put("uuid", PlayerUuid.toString());
        memberJsonObj.put("rank", Rank.toString());
        memberJsonObj.put("title", Title);
        memberJsonObj.put("date-join-ms", DateJoined.getTime());
        return memberJsonObj;
    }
}
