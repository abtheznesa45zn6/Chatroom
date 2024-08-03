package shared;

public class TextMessage extends Message {

    public TextMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }
    public String getUser() {
        return getStringAtIndex(1);
    }

    public String getText() {
        return getStringAtIndex(2);
    }

    @Override
    public String toString() {
        return getGroup() + " | " + getDate() + " | " + getUser() + " | " + getText();
    }
}