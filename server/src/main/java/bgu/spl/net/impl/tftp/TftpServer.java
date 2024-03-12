package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.BlockingConnections;
import bgu.spl.net.srv.Connections;

import java.io.IOException;
import java.util.function.Supplier;

public class TftpServer<T> extends BaseServer<T> {
    public TftpServer(int port, Supplier<BidiMessagingProtocol<T>> protocolFactory,
                      Supplier<MessageEncoderDecoder<T>> encdecFactory, Connections<T> connections) {
        super(port, protocolFactory, encdecFactory, connections);
    }

    @Override
    protected void execute(BlockingConnectionHandler<T> handler) {
        new Thread(handler).start();
    }

    public static void main(String[] args) {
        int port = 7777;

        if (args.length >= 1)
            port = Integer.parseInt(args[0]);

        TftpServer<TftpInstruction> server = new TftpServer<TftpInstruction>(port,
                TftpProtocol::new,
                TftpEncoderDecoder::new,
                new BlockingConnections());

        server.serve();

        try {
            server.close();
        } catch (IOException ignored) {
        }
    }
}
