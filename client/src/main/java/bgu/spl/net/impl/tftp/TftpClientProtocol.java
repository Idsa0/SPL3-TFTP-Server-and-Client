package bgu.spl.net.impl.tftp;

import java.io.IOException;

import bgu.spl.net.api.BidiMessagingProtocol;

public class TftpClientProtocol implements BidiMessagingProtocol<TftpInstruction> {

    private TftpProtocolStateMachine tftpProtocolStateMachine = new TftpProtocolStateMachine();
    private boolean shouldTerminate = false;

    public void addListener(ClientListener listener) {
        tftpProtocolStateMachine.addListener(listener);
    }

    @Override
    public void process(TftpInstruction message) {

        tftpProtocolStateMachine.execute(message);
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void startStateAndWait(TftpInstruction userInput) {
        tftpProtocolStateMachine.startStateAndWait(userInput);
    }

    public void terminate() {
        shouldTerminate = true;
        try {
            tftpProtocolStateMachine.close();
        } catch (IOException ignored) {
        }
    }
}
