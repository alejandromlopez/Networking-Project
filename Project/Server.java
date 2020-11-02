import java.io.*;
import java.net.*;

public class Server extends Thread {
    private final int lPort;
    private ServerSocket serverSocket;

    public Server(int port) {
        lPort = port;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(lPort) ;
            System.out.println("Server is listening on port " + lPort);
            int num = 1;
    
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected");
                    new Handler(socket, num).start();
                } catch (IOException e) {
					e.printStackTrace();
				}
                
                System.out.println("Client " + num + " is connected");
                num++;
            }
    
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
    }

    public class Handler extends Thread {  
        private final Socket socket;
        private final int num;
        private ObjectOutputStream output; 
        private ObjectInputStream input;  
        private String message;

        public Handler(Socket s, int n) {
            socket = s;
            num = n;
        }

        private void sendMessage(String message) {
            try {
				output.writeObject(message);
				output.flush();
				System.out.println("Send message: " + message + " to Client " + num);
			} catch(IOException ioException) {
				ioException.printStackTrace();
			}
        }

        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());

                try {
					while(true) {
						message = (String)input.readObject();
						System.out.println("Receive message: " + message + " from client " + num);
						sendMessage("I also say: " + message);
					}
				} catch(ClassNotFoundException classnot){
						System.err.println("Data received in unknown format");
				}
			} catch(IOException ioException){
					System.out.println("Disconnect with Client " + num);
			} finally {
				// close connections
				try {
					input.close();
					output.close();
					socket.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + num);
				}
			}
        }
    }
}
