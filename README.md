# Networking-Project

CNT4007C Peer-to-Peer network project. 
To ensure that this project runs, it requires you to have a public and private key 
authentication set up for SSH. If this is not set up the SSH for the machines will 
ask for passwords. Also, in StartRemotePeers.java, change line 11 to include your 
username for the CISE machines. Finally, transfer this Project directory into the 
remote machines using SFTP. Make sure that the name of the directory is "Project"
on the remote machine. To begin run ./compileJava in the working directory. 
Then you can run java StartRemotePeers. The alternative is to run the project on
the localhost.

## Message Payloads
Christopher Cooper, Kyle Cheng, Alejandro Lopez
- [x] choke 
- [x] unchoke
- [x] interested
- [x] not interested
- [x] have
- [x] bitfield
- [x] request
- [x] piece

## Protocols
Kyle Cheng, Christopher Cooper, Alejandro Lopez
- [X] handshake and bitfield
- [X] choke and unchoke
- [X] interested and not interested
- [X] request and piece

## Implementation Specifics
### Configuration Files
- [x] Common.cfg
- [x] PeerInfo.cfg

### Peer Process
Kyle Cheng, Christopher Cooper, Alejandro Lopez
- [X] Peer that started should make TCP connections to all peers started before it
- [X] When connected to at least one peer, starts to exchange pieces
- [X] Peer terminates when it finds out that all peers have downloaded the complete file

### Remote Machine 
Kyle Cheng, Alejandro Lopez
- [x] Reads file PeerInfo.cfg and starts peers in the file one by one
- [x] Terminates after starting all peers

### File Handling
Alejandro Lopez, Christopher Cooper
- [x] Should have subdirectories with 'peer_[peerID]'
- [x] Ensure that peer processes with a complete file actually contain the file before you start them

### Writing Log 
Kyle Cheng, Christopher Cooper
- [X] TCP Connection
- [X] Change of preferred neighbors
- [X] Change of optimistically unchoked neighbor
- [X] Unchoking
- [X] Choking
- [X] Receiving 'have' message
- [X] Receiving 'interested' message
- [X] Receiving 'not interested' message
- [X] Downloading a piece
- [X] Completion of download
