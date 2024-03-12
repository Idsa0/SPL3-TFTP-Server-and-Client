package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;

public abstract class TftpInstruction implements java.io.Serializable {
    public final Opcode opcode;

    public TftpInstruction(Opcode opcode) {
        this.opcode = opcode;
    }

    public enum Opcode {
        DEFAULT(0), RRQ(1), WRQ(2), DATA(3), ACK(4), ERROR(5), DIRQ(6), LOGRQ(7), DELRQ(8), BCAST(9), DISC(10);

        public final int value;

        Opcode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static TftpInstruction parse(byte[] bytes) {
        if (bytes.length < 2)
            return new ERROR(ERROR.ErrorCode.NOT_DEFINED, "Illegal input");

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
            switch ((int) bytes[1]) {
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
                    return new BCAST(data);
                case 10:
                    return new DISC(data);
                default:
                    return new ERROR(ERROR.ErrorCode.ILLEGAL_TFTP_OPERATION, "Invalid Command");
            }
        } catch (IllegalTFTPOperationException e) {
            return new ERROR(ERROR.ErrorCode.NOT_DEFINED, e.getMessage());
        }
    }

    public abstract byte[] toPacket();

    public byte[] opCodeBytes() {
        return new byte[]{(byte) (opcode.value() >> 8), (byte) (opcode.value() & 0xff)};
    }

    @Override
    public String toString(){
        return this.opcode.toString();
    }
}

class RRQ extends TftpInstruction {
    private final String filename;

    public RRQ(byte[] data) {
        this(new String(data, StandardCharsets.UTF_8).substring(0, data.length - 1));
    }

    public RRQ(String filename) {
        super(Opcode.RRQ);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] filenameBytes = filename.getBytes();
        byte[] output = new byte[filenameBytes.length + 3];
        System.arraycopy(opcb, 0, output, 0, 2);
        System.arraycopy(filenameBytes, 0, output, 2, filenameBytes.length);
        output[output.length - 1] = '\0';

        return output;
    }
}

class WRQ extends TftpInstruction {
    private final String filename;

    public WRQ(byte[] data) {
        this(new String(data, StandardCharsets.UTF_8).substring(0, data.length - 1));
    }

    public WRQ(String filename) {
        super(Opcode.WRQ);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] filenameBytes = filename.getBytes();
        byte[] output = new byte[filenameBytes.length + 3];
        System.arraycopy(opcb, 0, output, 0, 2);
        System.arraycopy(filenameBytes, 0, output, 2, filenameBytes.length);
        output[output.length - 1] = '\0';

        return output;
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

        if (data.length != packetSize + 4)
            throw new IllegalTFTPOperationException("Wrong packet Size as data");

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

    public static DATA buildData(byte[] dataToSend, short blockNumber) {
        byte[] tmpBytes = new byte[]{(byte) (dataToSend.length >> 8), (byte) (dataToSend.length & 0xff),
                (byte) (blockNumber >> 8), (byte) (blockNumber & 0xff)};

        byte[] inputBytes = new byte[dataToSend.length + 4];

        System.arraycopy(tmpBytes, 0, inputBytes, 0, 4);
        System.arraycopy(dataToSend, 0, inputBytes, 4, dataToSend.length);

        return new DATA(inputBytes);
    }

    @Override
    public byte[] toPacket() {
        byte[] output = new byte[data.length + 6];

        byte[] starter = new byte[]{(byte) (opcode.value() >> 8), (byte) (opcode.value() & 0xff),
                (byte) (packetSize >> 8), (byte) (packetSize & 0xff),
                (byte) (blockNumber >> 8), (byte) (blockNumber & 0xff)};

        System.arraycopy(starter, 0, output, 0, 6);
        System.arraycopy(data, 0, output, 6, packetSize);
        return output;
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
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        return new byte[]{opcb[0], opcb[1], (byte) (blockNumber >> 8), (byte) (blockNumber & 0xff)};
    }
}

class ERROR extends TftpInstruction {
    private final ErrorCode errorCode;
    private final String errorMsg;

    public ERROR(byte[] data) {
        super(Opcode.ERROR);

        if (data.length < 3)
            throw new IllegalTFTPOperationException("Invalid data");

        short ec = (short) ((data[0] << 8) | (data[1] & 0xff));

        if (ec < 0 || ec > 7)
            throw new IllegalTFTPOperationException("Invalid error code");

        this.errorCode = ErrorCode.values()[ec];

        
        this.errorMsg = new String(data, 2, data.length - 2, StandardCharsets.UTF_8);
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
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] errBytes = errorMsg.getBytes();
        byte[] output = new byte[errBytes.length + 5];

        byte[] starter = new byte[]{opcb[0], opcb[1],
                (byte) (errorCode.value() >> 8), (byte) (errorCode.value() & 0xff)};

        System.arraycopy(starter, 0, output, 0, 4);
        System.arraycopy(errBytes, 0, output, 4, errBytes.length);
        output[output.length - 1] = '\0';
        return output;
    }

    public enum ErrorCode {
        NOT_DEFINED(0), FILE_NOT_FOUND(1), ACCESS_VIOLATION(2), DISK_FULL(3), ILLEGAL_TFTP_OPERATION(4),
        FILE_ALREADY_EXISTS(5),
        USER_NOT_LOGGED_IN(6), USER_ALREADY_LOGGED_IN(7);

        private int value;

        ErrorCode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
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
    public byte[] toPacket() {
        return opCodeBytes();
    }

}

class LOGRQ extends TftpInstruction {
    private final String username;

    public LOGRQ(byte[] data) {
        this(new String(data, StandardCharsets.UTF_8).substring(0, data.length - 1));
    }
    public LOGRQ(String username) {
        super(Opcode.LOGRQ);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] usernameBytes = username.getBytes();
        byte[] output = new byte[usernameBytes.length + 3];
        System.arraycopy(opcb, 0, output, 0, 2);
        System.arraycopy(usernameBytes, 0, output, 2, usernameBytes.length);
        output[output.length - 1] = '\0';

        return output;
    }
}

class DELRQ extends TftpInstruction {
    private final String filename;

    public DELRQ(byte[] data) {
        this(new String(data, StandardCharsets.UTF_8).substring(0, data.length - 1));
    }

    public DELRQ(String filename) {
        super(Opcode.DELRQ);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] filenameBytes = filename.getBytes();
        byte[] output = new byte[filenameBytes.length + 3];
        System.arraycopy(opcb, 0, output, 0, 2);
        System.arraycopy(filenameBytes, 0, output, 2, filenameBytes.length);
        output[output.length - 1] = '\0';

        return output;
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

        if (data.length < 2)
            throw new IllegalTFTPOperationException("BCAST wrongly formatted");

        byte addByte = data[0];
        byte[] stringBytes = new byte[data.length - 1];
        System.arraycopy(data, 1, stringBytes, 0, stringBytes.length);

        if (addByte != 0 && addByte != 1)
            throw new IllegalTFTPOperationException("BCAST wrongly formatted");

        added = addByte == 1;
        filename = new String(stringBytes, StandardCharsets.UTF_8);
    }

    public boolean getAdded() {
        return added;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public byte[] toPacket() {
        byte[] opcb = opCodeBytes();
        byte[] fnBytes = filename.getBytes();
        byte[] output = new byte[fnBytes.length + 4];

        byte[] starter = new byte[]{opcb[0], opcb[1],
                (byte) (added ? 1 : 0)};

        System.arraycopy(starter, 0, output, 0, 3);
        System.arraycopy(fnBytes, 0, output, 3, fnBytes.length);
        output[output.length - 1] = '\0';
        return output;
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
    public byte[] toPacket() {
        return opCodeBytes();
    }
}
