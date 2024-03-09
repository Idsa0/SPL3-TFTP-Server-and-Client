package bgu.spl.net.api;


public interface BidiMessagingProtocol<T> {
    /**
     * Used to initiate the current client protocol with its personal connection ID and the connections implementation
     **/
    

    void process(T message);

    /**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();    
}
