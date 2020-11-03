package Messages;

public class Unchoke extends Message 
{
    public void Unchoke()
    {
        this.setMType( (byte)1);
        this.setMPayload(null);
    }
}