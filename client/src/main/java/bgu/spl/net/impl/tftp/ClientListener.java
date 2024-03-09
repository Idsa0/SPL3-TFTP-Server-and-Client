package bgu.spl.net.impl.tftp;

public interface ClientListener {
    void send(TftpInstruction instruction);
}
