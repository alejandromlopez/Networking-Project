public class Message {
    private int mLength;
    private byte mType;
    private byte[] mPayload;

    public Message(byte type, byte[] payload) {
        mLength = 1 + payload.length;
        mType = type;
        mPayload = payload;
    }

    public Message(byte type) {
        mLength = 1;
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
}