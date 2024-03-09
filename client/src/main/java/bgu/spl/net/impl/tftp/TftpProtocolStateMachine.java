package bgu.spl.net.impl.tftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

import bgu.spl.net.impl.tftp.IOHandler.IOHandlerMode;

public class TftpProtocolStateMachine {

    private State currentState;
    private IOHandler ioHandler;
    private TftpInstruction currentInstruction;
    private final Object userInputLock = new Object();
    private TftpInstruction outputResult;
    private short blockNumberExpected = 1;
    private ClientListener listener = null;
    private Queue<Byte> directoryListBuffer = new ArrayDeque<>();

    public TftpProtocolStateMachine() {
        this.currentState = State.DEFAULT;
    }

    public void addListener(ClientListener listener) {
        this.listener = listener;
    }

    public void execute(TftpInstruction instruction) {

        switch (instruction.opcode) {
            case ACK:
                handleACK((ACK) instruction);
                break;
            case BCAST:
                printOutBCAST((BCAST) instruction);
                return;
            case DATA:
                handleDATA((DATA) instruction);
                break;
            case ERROR:
                handleERROR((ERROR) instruction);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void handleERROR(ERROR instruction) {
        System.out.println("Received ERROR: ERROR code: " + instruction.getErrorCode().value() + " Message: "
                + instruction.getErrorMsg());
        reset();
        wakeUpKeyboardListener();
    }

    private void wakeUpKeyboardListener() {
        reset();
        synchronized (userInputLock) {
            userInputLock.notifyAll();
        }
    }

    private void handleDATA(DATA instruction) {
        switch (currentState) {
            case DIRQ:
                if (instruction.getBlockNumber() != blockNumberExpected)
                    throw new IllegalStateException();

                byte[] data = instruction.getData();
                for (byte b : data)
                    directoryListBuffer.add(b);

                // TODO find all .tosString() in code.
                if (instruction.getPacketSize() < 512) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : directoryListBuffer)
                        sb.append(new String(new byte[]{b}, StandardCharsets.UTF_8));

                    String[] fileList = sb.toString().split("\0");
                    for (String filename : fileList)
                        System.out.println("> " + filename);

                    listener.send(new ACK(blockNumberExpected));
                    reset();
                    wakeUpKeyboardListener();
                } else {
                    listener.send(new ACK(blockNumberExpected));
                    blockNumberExpected++;
                }
                break;
            case RRQ:
                if (instruction.getBlockNumber() != blockNumberExpected)
                    throw new IllegalStateException();

                if (blockNumberExpected == 1) {
                    ioHandler = new IOHandler(((RRQ) currentInstruction).getFilename(), IOHandlerMode.WRITE);
                    if (ioHandler.fileExists()) {
                        System.out.println("> file already exists!");
                        reset();
                        wakeUpKeyboardListener();
                        return;
                    }
                    try {
                        ioHandler.start();
                    } catch (FileNotFoundException ignored) {
                    }
                }
                try {
                    ioHandler.writeNext(instruction.getData());
                } catch (IOException e) {
                    System.out.println("> IO error on client side");
                    listener.send(new ACK((short) -1));
                    return;
                }

                listener.send(new ACK(blockNumberExpected));

                ++blockNumberExpected;

                if (ioHandler.isIODone()) {
                    reset();
                    wakeUpKeyboardListener();
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void handleACK(ACK instruction) {
        switch (currentState) {
            case DELRQ:
            case DISC:
            case LOGRQ:
                if (instruction.getBlockNumber() != 0)
                    throw new IllegalStateException();

                System.out.println("ACK 0");
                wakeUpKeyboardListener();
                break;
            case WRQ:
                if (instruction.getBlockNumber() != blockNumberExpected - 1)
                    throw new IllegalStateException();

                if (blockNumberExpected == 1) {
                    ioHandler = new IOHandler(((WRQ) currentInstruction).getFilename(), IOHandlerMode.READ);
                    try {
                        ioHandler.start();
                    } catch (FileNotFoundException e) {
                        System.out.println("File Not Found");
                        reset();
                        wakeUpKeyboardListener();
                        return;
                    }
                }

                if (ioHandler.isIODone()) {
                    System.out.println("WRQ completed");
                    reset();
                    wakeUpKeyboardListener();
                }
                DATA dataInstruction;

                // send data
                try {
                    dataInstruction = DATA.buildData(ioHandler.readNext(), blockNumberExpected);
                } catch (IOException e) {
                    System.out.println("IO error on client side");
                    dataInstruction = DATA.buildData(new byte[]{0, 0, 0, 0}, blockNumberExpected);
                    ioHandler.IODone();
                }

                blockNumberExpected++;
                listener.send(dataInstruction);

                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void reset() {
        blockNumberExpected = 1;
        currentInstruction = null;
        currentState = State.DEFAULT;
        directoryListBuffer.clear();
        if (ioHandler != null)
            ioHandler.freeResources();

        ioHandler = null;
    }

    private void printOutBCAST(BCAST instruction) {
        System.out.println(
                "File " + (instruction.getAdded() ? " added" : " deleted") + ": \"" + instruction.getFilename() + "\"");
    }

    private enum State {
        DEFAULT, LOGRQ, DELRQ, RRQ, WRQ, DIRQ, DISC
    }

    public TftpInstruction startStateAndWait(TftpInstruction userInput) {
        if (currentState != State.DEFAULT)
            throw new RuntimeException("client is attempting something illegal");

        currentInstruction = userInput;

        switch (userInput.opcode) {
            case DELRQ:
                currentState = State.DELRQ;
                break;
            case DIRQ:
                currentState = State.DIRQ;
                break;
            case DISC:
                currentState = State.DISC;
                break;
            case LOGRQ:
                currentState = State.LOGRQ;
                break;
            case RRQ:
                currentState = State.RRQ;
                break;
            case WRQ:
                currentState = State.WRQ;
                break;
            default:
                throw new RuntimeException("Something bad happened");
        }

        listener.send(userInput);

        synchronized (userInputLock) { // TODO race(the good one) condition 
            try {
                userInputLock.wait();
            } catch (InterruptedException e) {
                return null;
            }
        }

        return outputResult;
    }
}
