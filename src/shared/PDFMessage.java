package shared;

public class PDFMessage extends Message {

    public PDFMessage(ServerBefehl aktion, String[] stringArray) {
        super(aktion, stringArray);
    }

    public String getGroup() {
        return getStringAtIndex(0);
    }
}
