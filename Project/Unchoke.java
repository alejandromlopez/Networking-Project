
public class Unchoke extends Message {
    private int peerID;

    public Unchoke(int pid)
    {
        super((byte) 1, null);
        peerID = pid;
    }

    public int getPID(){
        return peerID;
    }
}