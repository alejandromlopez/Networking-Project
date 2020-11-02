import java.net.*;
import java.io.*;

public class Client implements Runnable {
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket socket;
    private final String hostname;
    private final int sPort;

    public Client(String host, int port) {
        hostname = host;
        sPort = port;
    }

    public void run() {
        try {
            socket = new Socket(hostname, sPort);
            input = new ObjectInputStream(socket.getInputStream());

            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
 
            String line = "This is a temporary message";
 
            System.out.println("Client sent: " + line);
            output.writeObject(line);

            String in = (String)input.readObject();
            System.out.println("Client received: " + in);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
				input.close();
				output.close();
				socket.close();
			} catch(IOException ioException) {
				ioException.printStackTrace();
			}
        }
    }
}
