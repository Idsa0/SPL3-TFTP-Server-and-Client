package bgu.spl.net.impl.tftp;

public class TftpClient {
    public static void main(String[] args) {
        String ip;
        if (args.length == 0)
            ip = "localhost";
        else
            ip = args[0];

        Listener listen = new Listener(ip, 7777, new TftpEncoderDecoder(), new TftpClientProtocol());
        KeyboardListener keyboardListener = new KeyboardListener(listen);
        
        new Thread(listen, "Communication thread").start(); // TODO synchronized their running so they are online at the same time.
        new Thread(keyboardListener, "Keyboard thread").start();
    }
}
