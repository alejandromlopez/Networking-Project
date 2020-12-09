
public class Choke extends Message {
    private int peerID;
    
    public Choke(int pid)
    {
        super((byte) 0);
        peerID = pid;
    }

    public int getPID(){
        return peerID;
    }
}