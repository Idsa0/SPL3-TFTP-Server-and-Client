package bgu.spl.net.impl.tftp;
// TODO package name clash

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class Listener implements Runnable, Closeable, ClientListener {
    private final TftpClientProtocol protocol;
    private final MessageEncoderDecoder<TftpInstruction> encdec; // TODO: can or should we move this back to generic?
    private Socket sock = null;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public Listener(String addressName, int port, MessageEncoderDecoder<TftpInstruction> reader,
                    TftpClientProtocol protocol) {
        try {
            this.sock = new Socket(addressName, port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.encdec = reader;
        this.protocol = protocol;
        this.protocol.addListener(this);
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            // TODO "read = in.read() >= 0" might make troubles in a bidi connection.
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {


                TftpInstruction nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) // TODO: if we are reading a packet, we should be blocking the keyboard thread
                    // from changing us.
                    protocol.process(nextMessage);
            }
        } catch (IOException ex) {
            System.out.println(ex.getCause());
        }
    }

    @Override
    public synchronized void close() throws IOException {
        connected = false;
        
        sock.close();
        
        
        // TODO: anything else to close?
    }

    @Override
    public synchronized void send(TftpInstruction msg) {
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void processUserInputAndWait(TftpInstruction userInput) {
        protocol.startStateAndWait(userInput);
    }

  
}
