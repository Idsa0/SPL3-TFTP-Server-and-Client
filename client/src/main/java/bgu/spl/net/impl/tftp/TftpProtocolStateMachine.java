package bgu.spl.net.impl.tftp;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

import bgu.spl.net.impl.tftp.IOHandler.IOHandlerMode;

public class TftpProtocolStateMachine implements Closeable {

    private State currentState;
    private IOHandler ioHandler;
    private TftpInstruction currentInstruction;
    private final Object userInputLock = new Object();
    private volatile boolean userInputLockFlag = false;
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
        System.out.println("> Error: code " + instruction.getErrorCode().value() + " message: "
                + instruction.getErrorMsg());

        if (currentState == State.RRQ)
            ioHandler.deleteFile();
        reset();
        wakeUpKeyboardListener();
    }

    private void wakeUpKeyboardListener() {

        synchronized (userInputLock) {
            if (!userInputLockFlag) {
                try {
                    userInputLock.wait();
                } catch (InterruptedException ignored) {

                }
            }
            userInputLockFlag = false;
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

                if (instruction.getPacketSize() < 512) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : directoryListBuffer)
                        sb.append(new String(new byte[] { b }, StandardCharsets.UTF_8));

                    String[] fileList = sb.toString().split("\0");
                    for (String filename : fileList)
                        System.out.println("> " + filename);

                    listener.send(new ACK(blockNumberExpected));
                    reset();
                    wakeUpKeyboardListener();
                    return;
                } else {
                    listener.send(new ACK(blockNumberExpected));
                    blockNumberExpected++;
                }
                break;

            case RRQ:
                if (instruction.getBlockNumber() != blockNumberExpected)
                    throw new IllegalStateException();

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
                    System.out.println("> RRQ " + ((RRQ) currentInstruction).getFilename() + " complete");
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

                System.out.println("> ACK 0");
                reset();
                wakeUpKeyboardListener();
                return;
            case WRQ:
                if (instruction.getBlockNumber() != blockNumberExpected - 1)
                    throw new IllegalStateException();

                System.out.println("> ACK " + instruction.getBlockNumber());
                if (ioHandler.isIODone()) {
                    System.out.println("> WRQ " + ((WRQ) currentInstruction).getFilename() + " complete");
                    reset();
                    wakeUpKeyboardListener();
                    return;
                }

                // send data
                DATA dataInstruction;
                try {
                    dataInstruction = DATA.buildData(ioHandler.readNext(), blockNumberExpected);
                } catch (IOException e) {
                    System.out.println("> IO error on client side");
                    dataInstruction = DATA.buildData(new byte[] { 0, 0, 0, 0 }, blockNumberExpected);
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
                "> BCAST " + (instruction.getAdded() ? "add" : "del") + ": \"" + instruction.getFilename() + "\"");
    }

    private enum State {
        DEFAULT, LOGRQ, DELRQ, RRQ, WRQ, DIRQ, DISC
    }

    public void startStateAndWait(TftpInstruction userInput) {
        if (currentState != State.DEFAULT)
            throw new RuntimeException("client is attempting something illegal");

        if (startState(userInput)) {

            synchronized (userInputLock) {
                try {
                    userInputLockFlag = true;
                    userInputLock.notifyAll();
                    ;
                    userInputLock.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /**
     * returns false if there is no packet to send, i.e. the job is cancelled.
     * Performs the start of the job otherwise and then sends appropriate packet.
     * 
     * @param userInput
     * @return
     */

    private boolean startState(TftpInstruction userInput) {

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
                ioHandler = new IOHandler(((RRQ) currentInstruction).getFilename(), IOHandlerMode.WRITE);
                if (ioHandler.fileExists()) {
                    System.out.println("> file already exists!");
                    reset();
                    return false;
                }
                try {
                    ioHandler.start();
                } catch (FileNotFoundException ignored) {
                }

                break;
            case WRQ:
                currentState = State.WRQ;
                ioHandler = new IOHandler(((WRQ) currentInstruction).getFilename(), IOHandlerMode.READ);
                try {
                    ioHandler.start();
                } catch (FileNotFoundException e) {
                    System.out.println("> file not found");
                    reset();
                    return false;
                }
                break;
            default:
                throw new IllegalStateException();
        }
        listener.send(userInput);
        return true;
    }

    @Override
    public void close() throws IOException {
        reset();
    }
}
