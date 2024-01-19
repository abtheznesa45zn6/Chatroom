package shared;

public class PictureMessage extends Message {
    public PictureMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }
}
