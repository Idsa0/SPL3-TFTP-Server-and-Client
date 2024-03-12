package bgu.spl.net.srv;

import java.io.Closeable;

public interface Connections<T> extends Iterable<ConnectionHandler<T>>, Closeable {
    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    int getUniqueID();

    boolean isUserLoggedIn(String username);

    boolean addUsername(String username, int connectionId);

    boolean removeUsername(String username);

    String getUsername(int connectionId);
}
