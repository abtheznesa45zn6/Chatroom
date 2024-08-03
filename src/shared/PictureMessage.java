package shared;

public class PictureMessage extends Message {
    public PictureMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }

    public String getPictureAsString() {
        return getStringAtIndex(1);
    }

    @Override
    public String toString() {
        return aktion + " | " + getGroup() + " | " + getDate() + " | " + "Größe des Strings: "+getPictureAsString().length();
    }
}