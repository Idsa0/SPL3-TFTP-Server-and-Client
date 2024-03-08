package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<TftpInstruction, byte[]> {
// TODO perhaps change the byte[] type to TftpInstruction once again.

    private int connectionId;
    private Connections<byte[]> connections;

    TftpProtocolStateMachine TftpProtocolStateMachine;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        TftpProtocolStateMachine = new TftpProtocolStateMachine(connections, connectionId);
    }

    @Override
    public void process(TftpInstruction instruction) {

        instruction.execute(this);

    }

    @Override
    public boolean shouldTerminate() {
        return TftpProtocolStateMachine.isTerminated();
        // TODO: where is this called?
    }

    public Connections<byte[]> getConnections() {
        return connections;
    }


    public int getConnectionId() {
        return connectionId; // TODO: should this be at the interface level defined?
    }
}
