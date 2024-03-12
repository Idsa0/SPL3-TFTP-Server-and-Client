package bgu.spl.net.impl.tftp;

import java.io.FileNotFoundException;
import java.io.IOException;

import bgu.spl.net.impl.tftp.ERROR.ErrorCode;
import bgu.spl.net.impl.tftp.IOHandler.IOHandlerMode;
import bgu.spl.net.impl.tftp.TftpInstruction.Opcode;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpProtocolStateMachine {
    final private Connections<TftpInstruction> connections;
    final private int connectionId;

    private State currentState;

    private IOHandler ioHandler;

    public TftpProtocolStateMachine(Connections<TftpInstruction> connections, int connectionId) {
        this.connections = connections;
        this.connectionId = connectionId;
        this.currentState = State.NOT_LOGGED_IN;
    }

    public void execute(TftpInstruction instruction) {
        if (instruction.opcode == TftpInstruction.Opcode.ERROR) { // if an error was generated during parsing.
            connections.send(connectionId, instruction);
            return;
        }
        if (instruction.opcode == TftpInstruction.Opcode.DISC) {
            if (currentState == State.NOT_LOGGED_IN)
                connections.send(connectionId, new ERROR(ErrorCode.USER_NOT_LOGGED_IN, "You're not even on!"));
            else
                connections.send(connectionId, new ACK((short) 0));
            terminate();
            return;
        }

        switch (currentState) {
            case NOT_LOGGED_IN:
                if (instruction.opcode == TftpInstruction.Opcode.LOGRQ) {
                    logIn((LOGRQ) instruction);
                } else {
                    connections.send(connectionId,
                            new ERROR(ERROR.ErrorCode.USER_NOT_LOGGED_IN, "User not Logged in"));
                }
                break;

            case LOGGED_IN:
                if (instruction.opcode == TftpInstruction.Opcode.RRQ)
                    beginRRQ((RRQ) instruction);
                else if (instruction.opcode == TftpInstruction.Opcode.WRQ)
                    beginWRQ((WRQ) instruction);
                else if (instruction.opcode == TftpInstruction.Opcode.DIRQ)
                    beginDIRQ((DIRQ) instruction);
                else if (instruction.opcode == TftpInstruction.Opcode.DELRQ)
                    deleteFileAndRespond((DELRQ) instruction);
                else if (instruction.opcode == TftpInstruction.Opcode.LOGRQ)
                    connections.send(connectionId,
                            new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "User already logged in"));
                else
                    connections.send(connectionId,
                            new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Illegal request at current state"));
                break;

            case RRQ:
                continueRRQ(instruction);
                break;

            case WRQ:
                continueWRQ(instruction);
                break;

            case DIRQ:
                continueDIRQ(instruction);
                break;

            case END:
                connections.send(connectionId, new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Protocol is done"));
                terminate();
                break;
        }
    }

    private void deleteFileAndRespond(DELRQ instruction) {
        String fileName = instruction.getFilename();
        ioHandler = new IOHandler(fileName, IOHandlerMode.DELETE);
        if (!ioHandler.fileExists())
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.FILE_NOT_FOUND, "No such file"));
        else {
            if (ioHandler.deleteFile()) {
                connections.send(connectionId, new ACK((short) 0));
                sendBCast(false, fileName);
            } else
                connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
        }
        endDataTransfer();
    }

    private void logIn(LOGRQ instruction) {
        String username = instruction.getUsername();
        if (connections.isUserLoggedIn(username)) {
            connections.send(connectionId,
                    new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "Another client is using this log in name"));
        } else {
            connections.addUsername(username, connectionId);
            currentState = State.LOGGED_IN;
            connections.send(connectionId, new ACK((short) 0));
        }
    }

    private void beginRRQ(RRQ instruction) {
        currentState = State.RRQ;
        ioHandler = new IOHandler(instruction.getFilename(), IOHandlerMode.READ);
        try {
            ioHandler.start();
        } catch (FileNotFoundException e) {
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.FILE_NOT_FOUND, "No such file"));
            endDataTransfer();
            return;
        }

        currentState = State.RRQ;
        try {
            short tmpBlockNum = ioHandler.getBlockNumber() ;
            connections.send(connectionId,
                    DATA.buildData(ioHandler.readNext(), tmpBlockNum));
        } catch (IOException e) {
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
        }

        
    }

    private void continueRRQ(TftpInstruction instruction) {
        if (instruction.opcode == TftpInstruction.Opcode.ACK) {
            if (!ioHandler.isIODone()){
                if (((ACK) instruction).getBlockNumber() == ioHandler.getBlockNumber() - 1)
                    try {
                        short tmpBlockNum = ioHandler.getBlockNumber() ;
                        connections.send(connectionId,
                                DATA.buildData(ioHandler.readNext(), tmpBlockNum));
                    } catch (IOException e) {
                        connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
                        endDataTransfer();
                        return;
                    }
                else {
                    connections.send(connectionId,
                            new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + ioHandler.getBlockNumber()));
                    endDataTransfer(); 
                }
            } else {
                endDataTransfer();
                return;
            }
        } else {
            connections.send(connectionId,
                    new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + ioHandler.getBlockNumber()));
            endDataTransfer(); 
            return;
        }
    }

    private void beginWRQ(WRQ instruction) {
        currentState = State.WRQ;

        ioHandler = new IOHandler(instruction.getFilename(), IOHandlerMode.WRITE);

        if (ioHandler.fileExists()) {
            connections.send(connectionId,
                    new ERROR(ERROR.ErrorCode.FILE_ALREADY_EXISTS, "File Already exists!"));
            endDataTransfer();
            return;
        }
        try {
            ioHandler.start();
        } catch (Exception ignored) {
            connections.send(connectionId, new ERROR(ErrorCode.NOT_DEFINED, "IO error"));
            endDataTransfer();
            return;
        }

        connections.send(connectionId, new ACK((short) 0));
    }

    private void continueWRQ(TftpInstruction instruction) {
        if (instruction.opcode == Opcode.DATA) {
            DATA dInstruction = (DATA) instruction;
            if (dInstruction.getBlockNumber() == ioHandler.getBlockNumber()) {
                try {
                    short tmpBlockNum = ioHandler.getBlockNumber() ;
                    ioHandler.writeNext(dInstruction.getData());

                    connections.send(connectionId, new ACK(tmpBlockNum));
                } catch (IOException e) {
                    connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
                    endDataTransfer();
                    return;
                }
            } else {
                ioHandler.deleteFile();
                connections.send(connectionId,
                        new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting DATA " + ioHandler.getBlockNumber()));
                endDataTransfer(); 
                return;

            }
        } else {
            ioHandler.deleteFile();
            connections.send(connectionId,
                    new ERROR(ErrorCode.NOT_DEFINED, "Expecting DATA" + ioHandler.getBlockNumber()));
            endDataTransfer();
            return;
        }

        if (ioHandler.isIODone()) {
            sendBCast(true, ioHandler.getFilename());
            endDataTransfer();
        }
    }

    private void beginDIRQ(DIRQ instruction) {
        currentState = State.DIRQ;

        ioHandler = new IOHandler(IOHandlerMode.DIR);
        try {
            ioHandler.start();
            short tmpBlockNum = ioHandler.getBlockNumber() ;
            connections.send(connectionId,
                    DATA.buildData(ioHandler.readNext(), tmpBlockNum));
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
        }
    }

    private void continueDIRQ(TftpInstruction instruction) {
        if (instruction.opcode == TftpInstruction.Opcode.ACK) {
            if (!ioHandler.isIODone()){
                if (((ACK) instruction).getBlockNumber() == ioHandler.getBlockNumber() - 1)
                    try {
                        short tmpBlockNum = ioHandler.getBlockNumber() ;
                        connections.send(connectionId, DATA.buildData(ioHandler.readNext(), tmpBlockNum));
                    } catch (IOException e) {
                        connections.send(connectionId, new ERROR(ERROR.ErrorCode.ACCESS_VIOLATION, "IO error"));
                        endDataTransfer();
                        return;
                    }

                else {
                    connections.send(connectionId,
                            new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + ioHandler.getBlockNumber()));
                    endDataTransfer(); 
                    return;
                }
            }
            else {
                endDataTransfer();
                return;
            }
        } else {
            connections.send(connectionId,
                    new ERROR(ERROR.ErrorCode.NOT_DEFINED, "expecting ACK " + ioHandler.getBlockNumber()));
            endDataTransfer(); 
        }
    }

    private void endDataTransfer() {
        ioHandler.freeResources();
        ioHandler = null; // only for extra safety
        currentState = State.LOGGED_IN;
    }

    private void terminate() {
        currentState = State.END;
        connections.removeUsername(connections.getUsername(connectionId));
    }

    public boolean isTerminated() {
        return currentState == State.END;
    }

    private void sendBCast(boolean added, String filename) {
        BCAST msg = new BCAST(added, filename);
        for (ConnectionHandler<TftpInstruction> ch : connections)
            if (connections.isUserLoggedIn(connections.getUsername(ch.getConnectionId())))
                ch.send(msg);
    }

    private enum State {
        NOT_LOGGED_IN, LOGGED_IN, RRQ, WRQ, DIRQ, END
    }
}
