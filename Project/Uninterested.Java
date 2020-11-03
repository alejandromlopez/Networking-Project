package Messages;

public class Uninterested extends Message 
{
    public void Uninterested()
    {
        this.setMType((byte)3);
        this.setMPayload(null);
    }
}