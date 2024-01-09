package shared;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 7L;

    ServerBefehl aktion;

    String[] stringArray;

    final LocalDateTime time;


    public Message(ServerBefehl aktion, String[] stringArray) {
        this.aktion = aktion;
        this.stringArray = stringArray;
        this.time = LocalDateTime.now();
    }

    public ServerBefehl getAktion() {
        return aktion;
    }

    public LocalDateTime getTime() {
        return time;
    }

    // Getter and setter methods for array elements
    public String getStringAtIndex(int index) {
        if (index >= 0 && index < stringArray.length) {
            return stringArray[index];
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "ServerBefehl=" + aktion +
                ", stringArray=" + Arrays.toString(stringArray) +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out)
    {
        try {
            out.defaultWriteObject();

        } catch (IOException e) {
            System.out.println("Message writeObject IOException");
            throw new RuntimeException(e);
        }
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in)
    {
        try {
            in.defaultReadObject();

        } catch (IOException e) {
            System.out.println("Message readObject IOException");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("Message ClassNotFoundException");
            throw new RuntimeException(e);
        }
    }

    @Serial
    private void readObjectNoData()
    {
        System.out.println("readObjectNoData()");
    }
}
