//import Messages.Uninterested;

public class MessageHandler {
    private final int _peerID;
    private final EventLog _eventLog;

    MessageHandler(EventLog eventLog, int peerID){
        _peerID = peerID;
        _eventLog = eventLog;
    }

    public String handle(Message msg){
        switch (msg.getMType()){
            //choke
            case 0: {
                _eventLog.choking(_peerID);
                return null;
            }
            //unchoke
            case 1:{
                _eventLog.unchoking(_peerID);
                return null;
            }
            //interested
            case 2:{
                _eventLog.recievingInterested(_peerID);
                return null;
            }
            //not interested
            case 3:{
                _eventLog.recievingNotInterested(_peerID);
                return null;
            }
            //have
            case 4:{
                boolean tmp = true;
                Have have = (Have) msg;
                final int pieceIdx = have.getPieceIdx();

                _eventLog.receivingHave(_peerID, pieceIdx);

                //TODO incorporate when to return un/interested
                if (tmp){
                    //return new Uninterested();
                }
                else{
                    //return new Interested();
                }
            }
            //bitfield
            case 5:{
                boolean tmp = true;
                if(tmp){
                    //return new Uninterested();
                }
                else{
                    //return new Interested();
                }
                return null;
            }
            //request
            case 6:{
                Request request = (Request) msg;
                byte[] piece = null;
                int requestIdx = request.getPieceIdx();
                //return new Piece(requestIdx, piece);
            }
            //piece
            case 7:{
                return null;
            }

            default:
                return "";
        }
    }
}