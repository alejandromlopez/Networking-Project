
public class Interested extends Message {
    private int peerID;
    public Interested(int pid)
    {
        super((byte) 2);
        peerID = pid;
    }

    public int getPID(){
        return peerID;
    }
}