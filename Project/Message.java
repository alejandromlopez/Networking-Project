public class Message 
{
    private static int mLength;
    private static byte mType;
    private static byte mPayload[];

    public Message(int len, byte type, byte[] payload) 
    {
        mLength = len;
        mType = type;
        mPayload = payload;
    }
}
