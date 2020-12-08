import java.io.Serializable;

public class Uninterested extends Message implements Serializable 
{
    private int peerID;
    public Uninterested(int pid)
    {
        super((byte) 3);
        peerID = pid;
    }

    public int getPID(){
        return peerID;
    }
}