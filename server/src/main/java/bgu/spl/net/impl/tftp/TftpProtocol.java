package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnections;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<TftpInstruction> {
    private int connectionId;
    private BlockingConnections connections;

    TftpProtocolStateMachine TftpProtocolStateMachine;

    @Override
    public void start(int connectionId, Connections<TftpInstruction> connections) {
        this.connectionId = connectionId;
        this.connections = (BlockingConnections) connections;
        TftpProtocolStateMachine = new TftpProtocolStateMachine(this.connections, connectionId);
    }

    @Override
    public void process(TftpInstruction instruction) {
        TftpProtocolStateMachine.execute(instruction);
    }

    @Override
    public boolean shouldTerminate() {
        return TftpProtocolStateMachine.isTerminated();
    }

    public Connections<TftpInstruction> getConnections() {
        return connections;
    }

    public int getConnectionId() {
        return connectionId;
    }
}
