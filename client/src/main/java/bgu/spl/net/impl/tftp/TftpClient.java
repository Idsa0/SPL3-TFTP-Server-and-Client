package bgu.spl.net.impl.tftp;

public class TftpClient {
    public static void main(String[] args) {
        String ip = "localhost";
        int port = 7777;

        if (args.length >= 1)
            ip = args[0];
        
        if (args.length >= 2)
            port = Integer.valueOf(args[1]);

        Listener listen = new Listener(ip, port, new TftpEncoderDecoder(), new TftpClientProtocol());
        KeyboardListener keyboardListener = new KeyboardListener(listen);
        
        new Thread(listen, "Communication thread").start(); // TODO synchronized their running so they are online at the same time.
        new Thread(keyboardListener, "Keyboard thread").start();
    }
}
