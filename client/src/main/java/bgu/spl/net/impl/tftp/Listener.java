package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class Listener implements Runnable, Closeable, ClientListener {
    private final TftpClientProtocol protocol;
    private final MessageEncoderDecoder<TftpInstruction> encdec;
    private Socket sock = null;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public Listener(String addressName, int port, MessageEncoderDecoder<TftpInstruction> reader,
                    TftpClientProtocol protocol) {
        try {
            this.sock = new Socket(addressName, port);
        } catch (IOException e) {
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

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                TftpInstruction nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null)
                    protocol.process(nextMessage);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized void close() throws IOException {
        connected = false;
        sock.close();
        protocol.terminate();
    }

    @Override
    public synchronized void send(TftpInstruction msg) {
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processUserInputAndWait(TftpInstruction userInput) {
        protocol.startStateAndWait(userInput);
    }

    
}
