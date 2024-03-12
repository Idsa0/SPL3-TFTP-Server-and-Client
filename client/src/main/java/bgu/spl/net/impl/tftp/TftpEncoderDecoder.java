package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpInstruction.Opcode;

import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpInstruction> {
    private final byte[] bytes = new byte[1024];
    private int len = 0;
    private short bitsLeft = 0;
    private short opCode;

    // TODO: can we move this, IOHandler, and other files which are repeated in server into an external library?
    @Override
    public TftpInstruction decodeNextByte(byte nextByte) {
        bytes[len++] = nextByte;

        if (len < 2)
            return null;

        if (len == 2) {
            opCode = (short) (((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff));
            if (opCode <= Opcode.DEFAULT.value() || opCode > Opcode.DISC.value())
                return getInstructionAndReset();
            if (opCode == Opcode.DIRQ.value() || opCode == Opcode.DISC.value())
                return getInstructionAndReset();
        }
        if (len > 2) {
            if (opCode == Opcode.RRQ.value() || opCode == Opcode.WRQ.value()
                    || opCode == Opcode.LOGRQ.value() || opCode == Opcode.DELRQ.value()) {
                // all opCodes that are 0-terminated
                if (nextByte == 0)
                    return getInstructionAndReset();
            }
            if (opCode == Opcode.DATA.value()) {
                if (len == 4) {
                    bitsLeft = (short) (((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff));
                    bitsLeft += 2; // for the blockNumber
                } else if (len > 4) {
                    --bitsLeft;
                    if (bitsLeft <= 0)
                        return getInstructionAndReset();
                }
            }
            if (opCode == Opcode.ACK.value() && len == 4)
                return getInstructionAndReset();
            if ((len > 4 && opCode == Opcode.ERROR.value()) ||
                    (len > 3 && opCode == Opcode.BCAST.value()))
                if (nextByte == 0)
                    return getInstructionAndReset();
        }
        return null;
    }

    public boolean isInPacketIO() {
        return len > 0;
    }

    private TftpInstruction getInstructionAndReset() {
        int tmp = len;
        len = 0;
        opCode = 0;
        return TftpInstruction.parse(Arrays.copyOfRange(bytes, 0, tmp));
    }

    @Override
    public byte[] encode(TftpInstruction message) {
        return message.toPacket();
    }
}
