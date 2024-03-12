package bgu.spl.net.impl.tftp;

import java.util.Scanner;

import bgu.spl.net.impl.tftp.TftpInstruction.Opcode;

public class KeyboardListener implements Runnable {

    private final Listener listener;
    private boolean shouldTerminate = false;

    public KeyboardListener(Listener listener) {
        this.listener = listener;
    }

    public void run() {
        Scanner s = new Scanner(System.in);
        TftpInstruction userInput;
        while (!shouldTerminate) {
            userInput = parseArgs(s.nextLine().split(" ")); // TODO test split(" ")
            if (userInput == null)
                System.out.println("Invalid instruction - please read manual");
            else {
                listener.processUserInputAndWait(userInput);
                endProcess(userInput);
            }
        }

        s.close();
        try {
            listener.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endProcess(TftpInstruction userInput) {
        if (userInput.opcode == Opcode.DISC)
            shouldTerminate = true;
    }

    

    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private TftpInstruction parseArgs(String[] args) {
        if (args.length > 2)
            return null;

        try {
            Opcode opcode = TftpInstruction.Opcode.valueOf(args[0]);

            switch (opcode) {
                case DELRQ:
                    if (args.length != 2)
                        return null;
                    return new DELRQ(args[1]);
                case DIRQ:
                    if (args.length != 1)
                        return null;
                    return new DIRQ();
                case DISC:
                    if (args.length != 1)
                        return null;
                    return new DISC();
                case LOGRQ:
                    if (args.length != 2)
                        return null;
                    return new LOGRQ(args[1]);
                case RRQ:
                    if (args.length != 2)
                        return null;
                    return new RRQ(args[1]);
                case WRQ:
                    if (args.length != 2)
                        return null;
                    return new WRQ(args[1]);
                default:
                    return null;
            }
        } catch (IllegalArgumentException noSuchOp) {
            return null;
        }
    }
}
