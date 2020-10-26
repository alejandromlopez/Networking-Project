# Networking-Project

CNT4007C Peer-to-Peer network project.

## Message Payloads
- [x] choke 
- [x] unchoke
- [x] interested
- [x] not interested
- [x] have
- [ ] bitfield
- [x] request
- [x] piece

## Protocols
- [ ] handshake and bitfield
- [ ] choke and unchoke
- [ ] interested and not interested
- [ ] request and piece

## Implementation Specifics
### Configuration Files
- [x] Common.cfg
- [x] PeerInfo.cfg

### Peer Process
- [ ] Peer that started should make TCP connections to all peers started before it
- [ ] When connected to at least one peer, starts to exchange pieces
- [ ] Peer terminates when it finds out that all peers have downloaded the complete file

### Remote Machine 
- [x] Reads file PeerInfo.cfg and starts peers in the file one by one
- [x] Terminates after starting all peers

### File Handling
- [x] Should have subdirectories with 'peer_[peerID]'
- [x] Ensure that peer processes with a complete file actually contain the file before you start them

### Writing Log
- [ ] TCP Connection
- [ ] Change of preferred neighbors
- [ ] Change of optimisticall unchoked neighbor
- [ ] Unchoking
- [ ] Choking
- [ ] Receiving 'have' message
- [ ] Receiving 'interested' message
- [ ] Receiving 'not interested' message
- [ ] Downloading a piece
- [ ] Completion of demo
