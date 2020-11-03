package Messages;

public class Interested extends Message 
{
    public void Interested()
    {
        this.setMType((byte) 2);
        this.setMPayload(null);
    }
}