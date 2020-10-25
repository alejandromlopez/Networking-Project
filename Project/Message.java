public class Message {
    private final int mLength;
    private final byte mType;
    private final byte[] mPayload;

    public Message(byte type, byte[] payload) {
        mLength = 1 + payload.length;
        mType = type;
        mPayload = payload;
    }

    public int getMLength() {
        return mLength;
    }

    public byte getMType() {
        return mType;
    }

    public byte[] getMPayload() {
        return mPayload;
    }
}
