package shared;

import server.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

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
        Logger.logVerbindungsaufbau(socket);
        establishStreamsAndRun();
    }


    protected void establishStreamsAndRun() {
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            dingeTun();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getClass());
            System.out.println("IO Exception bei establishStreamsAndRun");
        } finally {
            Logger.logVerbindungstrennung(socket, angemeldeterNutzer);

            // Nötig, um den Socket von ClientHandler zu schließen
            try {
                System.out.println("Socket is " + (this.socket.isClosed() ? "closed" : "not closed"));
                socket.close();
                System.out.println("Socket is " + (this.socket.isClosed() ? "closed" : "not closed"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    protected void listenAndExecute() {
        while (!isInterrupted()) {
            try {
                Message message = (Message) in.readObject();
                System.out.println("angekommene Message: " + message.toString());
                ausführenVon(message);
            } catch (SocketException e) {
                System.out.println("SocketException bei listenAndExecute");
                return;
            } catch (EOFException e) {
                e.printStackTrace();
                System.out.println("EOFException bei listenAndExecute");
                return;
            } catch (IOException e) {
                System.out.println("IOException bei listenAndExecute");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException");
            }
        }
    }

    protected abstract void ausführenVon(Message message);


    protected abstract void dingeTun();

    public void beenden(AbstractClass thread) {
        thread.interrupt();

        try {
            socket.shutdownOutput();
            socket.shutdownInput();
        } catch (IOException e) {
            System.out.println("IOException in beenden");
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void sendMessage(ServerBefehl aktion, String... strings) {
        try {
            Message message = new Message(aktion, strings);
            System.out.println("Message to send: "+message);
            Logger.log(message);
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        System.out.println("Message to send: "+message);
        try {
            Logger.log(message);
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAngemeldet() {
        return angemeldet;
    }

    public void setAngemeldet(boolean a) {angemeldet=a;}
    public String getAngemeldeterNutzer() {
        return angemeldeterNutzer;
    }
}