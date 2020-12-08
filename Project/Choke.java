import java.io.Serializable;

public class Choke extends Message implements Serializable 
{
    public Choke()
    {
        super((byte) 0, null);
    }
}