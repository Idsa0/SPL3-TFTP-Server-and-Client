package bgu.spl.net.srv;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import bgu.spl.net.impl.tftp.TftpInstruction;

public class BlockingConnections implements Connections<TftpInstruction> {
    private ConcurrentMap<Integer, ConnectionHandler<TftpInstruction>> handlers = new ConcurrentHashMap<Integer, ConnectionHandler<TftpInstruction>>();
    private ConcurrentMap<String, Integer> usernames = new ConcurrentHashMap<>();
    private int nextId = 0;

    @Override
    public void connect(int connectionId, ConnectionHandler<TftpInstruction> handler) {
        if (handlers.containsKey(connectionId))
            throw new RuntimeException("connectionId should be unique");
        handlers.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, TftpInstruction msg) {
        ConnectionHandler<TftpInstruction> ch = handlers.get(connectionId);
        if (ch == null)
            return false;

        ch.send(msg);
        return true;
    }

    @Override
    public void disconnect(int connectionId) {
        handlers.remove(connectionId);
    }

    @Override
    public boolean isUserLoggedIn(String username) {
        return usernames.containsKey(username);
    }

    @Override
    public boolean addUsername(String username, int connectionId) {
        if (usernames.containsValue(connectionId) || isUserLoggedIn(username))
            return false;

        usernames.put(username, connectionId);
        return true;
    }

    @Override
    public boolean removeUsername(String username) {
        if (!isUserLoggedIn(username))
            return false;

        usernames.remove(username);
        return true;
    }

    @Override
    public String getUsername(int connectionId) {
        for (String str : usernames.keySet())
            if (usernames.get(str) == connectionId)
                return str;

        return null;
    }

    @Override
    public Iterator<ConnectionHandler<TftpInstruction>> iterator() {
        return handlers.values().iterator();
    }

    @Override
    public int getUniqueID() {
        return nextId++;
    }

    @Override
    public void close() throws IOException {
        for (ConnectionHandler<TftpInstruction> ch : handlers.values())
            ch.close();
    }
}
