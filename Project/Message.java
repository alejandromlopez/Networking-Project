
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Message implements Serializable {
    private int mLength;
    private byte mType;
    private byte[] mPayload;

    public Message(byte type, byte[] payload) {
        mLength = 1 + payload.length;
        mType = type;
        mPayload = payload;
    }

    public Message(byte type) {
        mType = type;
    }

    public int getMLength() {
        return mLength;
    }

    public void setMLength(int len) {
        this.mLength = len;
    }

    public byte getMType() {
        return mType;
    }

    public void setMType(byte type) {
        this.mType = type;
    }

    public byte[] getMPayload() {
        return mPayload;
    }

    public void setMPayload(byte[] pl) {
        this.mPayload = pl;
    }

    public void read (DataInputStream in) throws IOException {
        if ((mPayload != null) && (mPayload.length) > 0) {
            in.readFully(mPayload, 0, mPayload.length);
        }
    }

    public void write(DataOutputStream o) throws IOException {
        o.writeInt (mLength);
        o.writeByte (mType);
        if ((mPayload != null) && (mPayload.length > 0)) {
            o.write (mPayload, 0, mPayload.length);
        }
    }
}