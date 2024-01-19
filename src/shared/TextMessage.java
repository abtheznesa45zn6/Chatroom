package shared;

import java.time.LocalDateTime;

public class TextMessage extends Message {

    public TextMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }

    public LocalDateTime getDate() {
        return time;
    }

    public String getUser() {
        return getStringAtIndex(1);
    }

    public String getText() {
        return getStringAtIndex(2);
    }

    @Override
    public String toString() {
        //return "Message{group=" + group + ",date=" + date + ",user=" + user + ",text=" + text + "}";
        return getGroup() + " | " + getDate() + " | " + getUser() + " | " + getText();
    }
}