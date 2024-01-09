package shared;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public abstract class AbstractClass extends Thread {
    protected final Socket socket;
    protected boolean angemeldet = false;
    protected String angemeldeterNutzer;
    protected ObjectInputStream in;
    protected ObjectOutputStream out;


    public AbstractClass(Socket socket) {
        this.socket = socket;
    }



    @Override
    public void run() {
        establishStreamsAndRun();
    }


    protected void establishStreamsAndRun() {
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            dingeTun();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            // Nötig, um den Socket von ClientHandler zu schließen
            try {
                System.out.println("Socket is "+(this.socket.isClosed()?"closed" : "not closed"));
                this.socket.close();
                System.out.println("Socket is "+(this.socket.isClosed()?"closed" : "not closed"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    protected void listenAndExecute() {
        while (!isInterrupted()) {
            try {
                Message message = (Message) in.readObject(); // decorate with bufferedstream, then use if(.available>0)
                System.out.println("angekommene Message: "+message.toString());
                ausführenVon(message);
            } catch (IOException e) {
                System.out.println("RuntimeException");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException");
            }
        }
    }

    protected abstract void ausführenVon(Message message);


    protected abstract void dingeTun();

    public void beenden() {
        Thread.currentThread().interrupt();
        try {
            sleep(2000);
            this.socket.shutdownOutput();
            this.socket.shutdownInput();
            this.socket.close();
        } catch (InterruptedException e) {
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (this.socket != null && !this.socket.isClosed()) {
                    this.socket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };



    protected void sendMessage(ServerBefehl aktion, String... strings) {
        try {
            Message message = new Message(aktion, strings);
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendMessage(Message message) {
        try {
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected String readText()  {
        try {
            return (String) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception bei readText");
            //Thread.currentThread().interrupt();
        }
        return null;
    }

    protected Integer readInt()  {
        try {
            return (Integer) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception bei readText");
            //Thread.currentThread().interrupt();
        }
        return null;
    }

    protected void write (String text) {
        try {
            out.writeObject(text);
        } catch (IOException e) {
            System.out.println("Exception bei write String");
        }
    }

    protected void write (int zahl) {
        try {
            out.writeObject(zahl);
        } catch (IOException e) {
            System.out.println("Exception bei write Integer");
        }
    }
}
