import java.io.Serializable;

public class Unchoke extends Message implements Serializable 
{
    public Unchoke()
    {
        super((byte) 1, null);
    }
}