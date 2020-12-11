import java.nio.ByteBuffer;

public class Request extends Message {

    public Request(int pIdx) {
        super((byte)6, ByteBuffer.allocate(4).putInt(pIdx).array());
    }
}
