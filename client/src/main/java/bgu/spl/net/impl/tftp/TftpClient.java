package bgu.spl.net.impl.tftp;

public class TftpClient {
    public static void main(String[] args) {
        String ip = "localhost";
        int port = 7777;
        
        if (args.length == 0) {
            ip = "localhost";
            port = 7777;
        }
        else
            ip = args[0];

        
        // TODO weird null bug??
        Listener listen = new Listener(ip, port, new TftpEncoderDecoder(), new TftpClientProtocol());
        KeyboardListener keyboardListener = new KeyboardListener(listen);

        new Thread(listen, "Communication thread").start(); // TODO synchronized their running so they are online at the same time.
        new Thread(keyboardListener, "Keyboard thread").start();
    }
}
