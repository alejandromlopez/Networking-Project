package Messages;

public class Choke extends Message 
{
    public void Choke()
    {
        this.setMType((byte) 0);
        this.setMPayload(null);
    }
}