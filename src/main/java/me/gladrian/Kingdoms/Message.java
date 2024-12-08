package me.gladrian.Kingdoms;

import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

public class Message implements Comparable<Message> {
    public @Nullable UUID FromUuid;
    public UUID ToUuid;
    public String Content;
    public Date DateSent;

    public int compareTo(Message other) {
        var diff = this.DateSent.getTime() - other.DateSent.getTime();
        if (diff < 0) return -1;
        if (diff > 0) return 1;
        return 0;
    }

    public static Message fromJson(JSONObject messageJsonObj) {
        var message = new Message();
        message.ToUuid = UUID.fromString(messageJsonObj.getString("to-uuid"));
        message.Content = messageJsonObj.getString("content");
        message.DateSent = new Date(messageJsonObj.getLong("date-sent"));
        if (messageJsonObj.has("from-uuid"))
            message.FromUuid = UUID.fromString(messageJsonObj.getString("from-uuid"));
        return message;
    }

    public JSONObject toJson() {
        var messageJsonObj = new JSONObject();
        messageJsonObj.put("to-uuid", ToUuid.toString());
        messageJsonObj.put("content", Content);
        messageJsonObj.put("date-sent", DateSent.getTime());
        if (FromUuid != null)
            messageJsonObj.put("from-uuid", FromUuid.toString());
        return messageJsonObj;
    }
}
