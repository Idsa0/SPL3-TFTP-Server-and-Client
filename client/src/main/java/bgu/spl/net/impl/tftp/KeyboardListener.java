package bgu.spl.net.impl.tftp;

import java.io.IOException;
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
            userInput = parseArgs(s.nextLine().split(" "));
            if (userInput == null)
                System.out.println("> Invalid instruction - please read manual");
            else {
                listener.processUserInputAndWait(userInput);
                endProcess(userInput);
            }
        }

        s.close();
        try {
            listener.close();
        } catch (IOException ignored) {
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
        String name = "";
        if (args.length > 2){
            String[] tmpArr = new String[args.length-1];
            System.arraycopy(args, 1, tmpArr, 0, args.length - 1);
            name = String.join(" ", tmpArr);
        } else if (args.length == 2)
            name = args[1];
        try {
            Opcode opcode = TftpInstruction.Opcode.valueOf(args[0]);

            switch (opcode) {
                case DELRQ:
                    if (args.length < 2)
                        return null;
                    return new DELRQ(name);
                case DIRQ:
                    if (args.length != 1)
                        return null;
                    return new DIRQ();
                case DISC:
                    if (args.length != 1)
                        return null;
                    return new DISC();
                case LOGRQ:
                    if (args.length < 2)
                        return null;
                    return new LOGRQ(name);
                case RRQ:
                    if (args.length < 2)
                        return null;
                    return new RRQ(name);
                case WRQ:
                    if (args.length < 2)
                        return null;
                    return new WRQ(name);
                default:
                    return null;
            }
        } catch (IllegalArgumentException noSuchOp) {
            return null;
        }
    }
}
