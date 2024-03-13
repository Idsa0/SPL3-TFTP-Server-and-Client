package bgu.spl.net.impl.tftp;

public class TftpClient {
    public static void main(String[] args) {
        String ip = "localhost";
        int port = 7777;

        if (args.length >= 1)
            ip = args[0];

        if (args.length >= 2)
            port = Integer.parseInt(args[1]);

        Listener communicatorThread = new Listener(ip, port, new TftpEncoderDecoder(), new TftpClientProtocol());
        KeyboardListener keyboardThread = new KeyboardListener(communicatorThread);

        new Thread(communicatorThread, "Communication thread").start();
        new Thread(keyboardThread, "Keyboard thread").start();
    }
}
