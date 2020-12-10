import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Reader implements Runnable {
    private ObjectInputStream in;
    private Socket socket;

    public Reader(Socket s) {
        socket = s;
    }

    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());

            byte[] lenBuf = new byte[4];
            in.read(lenBuf, 0, 4);
            int length = ByteBuffer.wrap(lenBuf).getInt();

            byte[] messageType = new byte[1];
            in.read(messageType, 0, 1);

            byte[] payload = new byte[length];
            in.read(payload, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
