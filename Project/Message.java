public class Message {
    private final int mLength;
    private final byte mType;
    private final String mPayload;

    public Message(byte type, String payload) {
        mLength = 1 + payload.getBytes().length;
        mType = type;
        mPayload = payload;
    }

    public int getMLength() {
        return mLength;
    }

    public byte getMType() {
        return mType;
    }

    public String getMPayload() {
        return mPayload;
    }
}
