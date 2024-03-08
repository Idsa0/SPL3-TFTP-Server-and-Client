package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;

public interface BidiMessagingProtocol<T, S> {
    /**
     * Used to initiate the current client protocol with its personal connection ID and the connections implementation
     **/
    void start(int connectionId, Connections<S> connections);

    void process(T message);

    /**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
