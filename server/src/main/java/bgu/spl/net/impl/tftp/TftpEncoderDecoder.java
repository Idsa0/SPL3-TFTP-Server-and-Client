package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpInstruction> {
    //TODO: Implement here the TFTP encoder and decoder

    private final byte[] bytes = new byte[1024];
    private int len = 0;
    private short bitsLeft = 0;
    private short opCode;


    @Override
    public TftpInstruction decodeNextByte(byte nextByte) {
        bytes[len++] = nextByte;

        if (len < 2)
            return null;

        if (len == 2) {
            opCode = (short) (((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff));
            if (opCode < 1 || opCode > 10) {  // TODO magic number
                return getInstructionAndReset();
            }
            if (opCode == 6 || opCode == 10) { // DIRQ or DISC //TODO magic number
                return getInstructionAndReset();
            }
        }

        if (len > 2) {
            if (opCode == 1 || opCode == 2 || opCode == 5 || opCode == 7 || opCode == 8 || opCode == 9) {
                // all opCodes that are 0-terminated
                if (nextByte == 0) {
                    return getInstructionAndReset();
                }
            }
            if (opCode == 3) {
                // DATA
                if (len == 4) {
                    bitsLeft = (short) (((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff));
                    bitsLeft += 2; // for the blockNumber
                } else {
                    bitsLeft--;
                    if (bitsLeft <= 0) {
                        return getInstructionAndReset();
                    }
                }
            }

            if (opCode == 4) {
                // ACK
                if (len == 4) {
                    return getInstructionAndReset();
                }
            }
        }
        return null;
    }

    private TftpInstruction getInstructionAndReset() {
        int tmp = len;
        len = 0;
        opCode = 0;
        return TftpInstruction.parse(Arrays.copyOfRange(bytes, 0, tmp));
    }

    @Override
    public byte[] encode(TftpInstruction message) {
        //TODO: implement this
        return null;
    }

//    private Serializable serializeObject(TftpInstruction instruction) {
//        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
//            out.writeObject(instruction);
//            return bos.toByteArray();
//        } catch (Exception ex) {
//            throw new IllegalArgumentException("cannot serialize object", ex);
//        }
//    } // TODO: remove if unneeded


}
