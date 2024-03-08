package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

import java.util.function.Function;

public class TftpProtocolStateMachine {


    final private Connections<byte[]> connections;
    final private int connectionId;

    private State currentState;

    private String fileName = null;

    private int blockNumber = 0;

    public TftpProtocolStateMachine(Connections<byte[]> connections, int connectionId) {
        this.connections = connections;
        this.connectionId = connectionId;
        State currentState = State.NOT_LOGGED_IN;
    }

    public void execute(TftpInstruction instruction) {
        if (instruction.opcode == TftpInstruction.Opcode.ERROR) { // if an error was generated during parsing.
            connections.send(connectionId, ((ERROR)instruction).toPacket());
            return;
        }
        if (instruction.opcode == TftpInstruction.Opcode.DISC) {
            terminate();
            return;
        }

        switch (currentState) {
            case NOT_LOGGED_IN:
                if (instruction.opcode == TftpInstruction.Opcode.LOGRQ){
                    logIn((LOGRQ) instruction);
                } else {
                    connections.send(connectionId, new ERROR(ERROR.ErrorCode.USER_NOT_LOGGED_IN, "User not Logged in").toPacket());
                }
                break;
            case LOGGED_IN:
                if (instruction.opcode == TftpInstruction.Opcode.RRQ){
                    currentState = State.RRQ;  // TODO insert into begin
                    beginRRQ((RRQ) instruction);
                    // TODO RRQ, WRQ, DRQ, starts here.
                } else if (instruction.opcode == TftpInstruction.Opcode.WRQ){
                    beginWRQ((WRQ) instruction);
                    currentState = State.WRQ;
                } else if (instruction.opcode == TftpInstruction.Opcode.DIRQ){
                    beginDIRQ((DIRQ) instruction);
                    currentState = State.DIRQ;
                } else if (instruction.opcode == TftpInstruction.Opcode.LOGRQ){
                    connections.send(connectionId, new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "User already logged in").toPacket());
                } else if (instruction.opcode == TftpInstruction.Opcode.DELRQ){
                    deleteFileAndRespond((DELRQ) instruction);
                }  else {
                    connections.send(connectionId, new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Illegal request at current state").toPacket());
                }
                break;

            case RRQ:
                if(continueRRQ(instruction))
                    endRRQ();
                break;

            case WRQ:
                if(continueWRQ(instruction))
                    endWRQ();
                break;

            case DIRQ:
                if(continueDIRQ(instruction))
                    endDIRQ();
                break;

            case END:
                connections.send(connectionId, new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Protocol is done").toPacket());
                terminate();
                break;


        }
    }

    private void deleteFileAndRespond(DELRQ instruction) {
        // TODO implement
    }

    private void terminate() {

        currentState = State.END;
        connections.removeUserName(connections.getUserName(connectionId));


    }

    public boolean isTerminated(){
        return currentState == State.END;
    }

    private void logIn(LOGRQ instruction) {
        String username = instruction.getUsername();
        if (connections.isUserLoggedIn(username)) {
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "Another client is using this log in name").toPacket());
        } else {
            connections.addUserName(username, connectionId);
            connections.send(connectionId, new ACK((short) 0).toPacket());
        }
    }

    //connections.send(protocol.getConnectionId/(), new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "User already logged in").toPacket()); // TODO: Unsure if a new client can use a name that is already logged in

    private void beginRRQ(RRQ instruction) {
        currentState = State.RRQ;
        fileName = instruction.getFilename();
        blockNumber = 1;

        // TODO : send first file data packet
        // TODO implement
    }

    private boolean continueRRQ(TftpInstruction instruction) {
        // returns true if file transfer is done.

       if (instruction.opcode == TftpInstruction.Opcode.ACK) {
           if (((ACK) instruction).getBlockNumber() == blockNumber) {
               // TODO if file transfer is done, return true
               blockNumber++;
               // TODO send next data packet
               return false;
           } else {
               connections.send(connectionId, new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + blockNumber).toPacket());
               return true; // TODO should a wrong ACK terminate the transfer or do we continue in transfer, and wait for the right package?
           }
       } else{
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + blockNumber).toPacket());
            return true; // TODO should a wrong ACK terminate the transfer or do we continue in transfer, and wait for the right package?
       }
    }

    private void endRRQ() {
        // TODO close file readers and resources, etc.
        currentState = State.LOGGED_IN;
    }

    private void beginWRQ(WRQ instruction) {
        fileName = instruction.getFilename();
        blockNumber = 0;

        // TODO : send first ACK
        // TODO implement
    }


    private boolean continueWRQ(TftpInstruction instruction) {
        // returns true if file transfer is done.
        // TODO implement
        return false;
    }

    private void endWRQ() {
        // TODO close file writers and resources, etc.
        currentState = State.LOGGED_IN;
    }

    private void beginDIRQ(DIRQ instruction) {
        // TODO implement
    }

    private boolean continueDIRQ(TftpInstruction instruction) {
        // returns true if file transfer is done.
        return false;
        // TODO implement
    }

    private void endDIRQ() {
        // TODO close file readers and resources, etc.
        currentState = State.LOGGED_IN;
    }




    private enum State {
        NOT_LOGGED_IN, LOGGED_IN, RRQ, WRQ,  DIRQ, END
    }



}


