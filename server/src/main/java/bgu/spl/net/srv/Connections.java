package bgu.spl.net.srv;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);


    boolean isUserLoggedIn(String userName); // TODO: remember to do this concurrent

    boolean addUserName(String userName, int connectionId);

    boolean removeUserName(String userName);

    String getUserName(int connectionId);

    // TODO we can maybe take those 3 functions to a interface

}
