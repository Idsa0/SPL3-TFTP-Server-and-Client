package bgu.spl.net.impl.tftp;

// TODO wherever there is a byte[] to short cast add 0xff to the first byte

import bgu.spl.net.srv.Connections;

public abstract class TftpInstruction implements java.io.Serializable {
    public final Opcode opcode;

    public TftpInstruction(Opcode opcode) {
        this.opcode = opcode;
    }

    public enum Opcode {
        DEFAULT, RRQ, WRQ, DATA, ACK, ERROR, DIRQ, LOGRQ, DELRQ, BCAST, DISC
    }

    public abstract void execute(TftpProtocol protocol);

    public static TftpInstruction parse(byte[] bytes) {
        if (bytes.length < 2)
            return new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Illegal input"); // TODO

        byte[] data = new byte[bytes.length - 2];
        System.arraycopy(bytes, 2, data, 0, data.length);

        byte[] opcode = new byte[2];
        System.arraycopy(bytes, 0, opcode, 0, 2);

        return parse(opcode, data);
    }

    public static TftpInstruction parse(byte[] bytes, byte[] data) {
        if (bytes.length < 2 || bytes[0] != 0 || bytes[1] < 1 || bytes[1] > 10)
            return new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Wrong Data");

        try {
            switch (bytes[1]) { // TODO make sure each case is correct
                case 1:
                    return new RRQ(data);
                case 2:
                    return new WRQ(data);
                case 3:
                    return new DATA(data);
                case 4:
                    return new ACK(data);
                case 5:
                    return new ERROR(data);
                case 6:
                    return new DIRQ(data);
                case 7:
                    return new LOGRQ(data);
                case 8:
                    return new DELRQ(data);
                case 9:
                    return new BCAST(data); // TODO bcasts are never to be created by the user, leave this here for debugging and replace with error with code 0
                case 10:
                    return new DISC(data);
                default:
                    return new ERROR(ERROR.ErrorCode.ILLEGAL_TFTP_OPERATION, "Invalid Command");
            }
        } catch (IllegalTFTPOperationException e) {
            return new ERROR(ERROR.ErrorCode.NOT_DEFINED, e.getMessage());
        }
    }
}

class RRQ extends TftpInstruction {
    private final String filename;

    public RRQ(byte[] data) {
        super(Opcode.RRQ);
        this.filename = new String(data).substring(0, data.length - 1);
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class WRQ extends TftpInstruction {
    private final String filename;

    public WRQ(byte[] data) {
        super(Opcode.WRQ);
        this.filename = new String(data).substring(0, data.length - 1);
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class DATA extends TftpInstruction {
    private final short packetSize;
    private final short blockNumber;
    private final byte[] data;

    public DATA(byte[] data) {
        super(Opcode.DATA);

        if (data.length < 4)
            throw new IllegalTFTPOperationException("Invalid data");

        this.packetSize = (short) ((data[0] << 8) | (data[1] & 0xff));

        if (packetSize < 0 || packetSize > 512)
            throw new IllegalTFTPOperationException("Invalid packet size");

        this.blockNumber = (short) ((data[2] << 8) | (data[3] & 0xff));

        if (blockNumber < 0)
            throw new IllegalTFTPOperationException("Invalid block number");

        this.data = new byte[data.length - 4];
        System.arraycopy(data, 4, this.data, 0, this.data.length);
    }

    public short getPacketSize() {
        return packetSize;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class ACK extends TftpInstruction {
    private final short blockNumber;

    public ACK(byte[] data) {
        super(Opcode.ACK);

        if (data.length < 2)
            throw new IllegalTFTPOperationException("Invalid data");

        this.blockNumber = (short) ((data[0] << 8) | (data[1] & 0xff));

        if (blockNumber < 0)
            throw new IllegalTFTPOperationException("Invalid block number");
    }

    public ACK(short blockNumber) {
        super(Opcode.ACK);
        this.blockNumber = blockNumber;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }

    public byte[] toPacket() {
        // TODO implement
        return null;
    }
}

class ERROR extends TftpInstruction {
    private final ErrorCode errorCode;
    private final String errorMsg;

    public ERROR(byte[] data) {
        super(Opcode.ERROR);

        if (data.length < 3)
            throw new IllegalTFTPOperationException("Invalid data"); // TODO < 4?

        short ec = (short) ((data[0] << 8) | (data[1] & 0xff));

        if (ec < 0 || ec > 7)
            throw new IllegalTFTPOperationException("Invalid error code");

        this.errorCode = ErrorCode.values()[ec];

        this.errorMsg = new String(data, 2, data.length - 2);
    }

    public ERROR(ErrorCode errorCode, String errorMsg) {
        super(Opcode.ERROR);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }

    public byte[] toPacket() {
        // TODO implement
        return null;
    }

    public enum ErrorCode {
        NOT_DEFINED, FILE_NOT_FOUND, ACCESS_VIOLATION, DISK_FULL, ILLEGAL_TFTP_OPERATION, FILE_ALREADY_EXISTS, USER_NOT_LOGGED_IN, USER_ALREADY_LOGGED_IN
    }
}

class DIRQ extends TftpInstruction {
    public DIRQ() {
        super(Opcode.DIRQ);
    }

    public DIRQ(byte[] data) {
        this();
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class LOGRQ extends TftpInstruction {
    private final String username;

    public LOGRQ(byte[] data) {
        super(Opcode.LOGRQ);
        this.username = new String(data).substring(0, data.length - 1);
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void execute(TftpProtocol protocol) { // TODO what if this is the second packet? check instructions pdf
//        Connections<byte[]> connections = protocol.getConnections();
//        if (protocol.isLoggedIn()) // TODO: Unsure if a new client can use a name that is already logged in
//            connections.send(protocol.getConnectionId(), new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "User already logged in").toPacket());
//        else {
//            if (connections.isUserLoggedIn(username)) {
//                connections.send(protocol.getConnectionId(), new ERROR(ERROR.ErrorCode.USER_ALREADY_LOGGED_IN, "Another client is using this log in name").toPacket());
//            } else {
//                protocol.LogIn();
//                connections.addUserName(username, protocol.getConnectionId());
//                connections.send(protocol.getConnectionId(), new ACK((short) 0).toPacket());
//            }
//        }
    } // TODO remove all execute in instructions.
}

class DELRQ extends TftpInstruction {
    private final String filename;

    public DELRQ(byte[] data) {
        super(Opcode.DELRQ);
        this.filename = new String(data).substring(0, data.length - 1);
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class BCAST extends TftpInstruction {
    private final boolean added;
    private final String filename;

    public BCAST(boolean added, String filename) {
        super(Opcode.BCAST);
        this.added = added;
        this.filename = filename;
    }

    public BCAST(byte[] data) {
        super(Opcode.BCAST);
        throw new IllegalTFTPOperationException("Client should not send BCAST"); // tODO i hate this
//        super(Opcode.BCAST);
//
//        if (data.length < 2 || (data[0] > 1 || data[0] < 0)) // TODO can the filename be empty?
//            throw new IllegalTFTPOperationException("Invalid data");
//
//        this.added = data[0] == 1;
//        this.filename = new String(data, 1, data.length - 1);
    }

    public boolean getAdded() {
        return added;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public void execute(TftpProtocol protocol) {

    }
}

class DISC extends TftpInstruction {
    public DISC() {
        super(Opcode.DISC);
    }

    public DISC(byte[] data) {
        this();
    }

    @Override
    public void execute(TftpProtocol protocol) {
//        Connections<byte[]> connections = protocol.getConnections();
//        if (protocol.isLoggedIn()) {
//            connections.removeUserName(connections.getUserName(protocol.getConnectionId()));
//            connections.send(protocol.getConnectionId(), new ACK((short) 0).toPacket()); // todo make boolean ack constructor
//        } else
//            connections.send(protocol.getConnectionId(), new ERROR(ERROR.ErrorCode.USER_NOT_LOGGED_IN, "User not logged in").toPacket());
//        protocol.terminate();
    }
}
