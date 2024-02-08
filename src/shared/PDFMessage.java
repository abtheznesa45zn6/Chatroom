package shared;

public class PDFMessage extends Message {

    public PDFMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }

    public String getPDFAsString() {
        return getStringAtIndex(1);
    }

    @Override
    public String toString() {
        return aktion + " | " + getGroup() + " | " + getDate() + " | " + "Größe des Strings: "+getPDFAsString().length();
    }
}
