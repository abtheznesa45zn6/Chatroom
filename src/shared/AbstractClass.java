package shared;

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
        establishStreamsAndRun();
    }


    protected void establishStreamsAndRun() {
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            dingeTun();

        } catch (IOException ignored) {
        } finally {
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
            } catch (SocketException ignored) {
            } catch (EOFException e) {
                System.out.println("EOFException bei listenAndExecute");
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
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

    protected void abmelden(AbstractClass thread) {
    }


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


    protected String readText() {
        try {
            return (String) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception bei readText");
            //Thread.currentThread().interrupt();
        }
        return null;
    }

    protected Integer readInt() {
        try {
            return (Integer) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception bei readText");
            //Thread.currentThread().interrupt();
        }
        return null;
    }

    protected void write(String text) {
        try {
            out.writeObject(text);
        } catch (IOException e) {
            System.out.println("Exception bei write String");
        }
    }

    protected void write(int zahl) {
        try {
            out.writeObject(zahl);
        } catch (IOException e) {
            System.out.println("Exception bei write Integer");
        }
    }

    public boolean getAngemeldet() {
        return angemeldet;
    }
}