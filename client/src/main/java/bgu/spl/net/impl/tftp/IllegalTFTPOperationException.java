package bgu.spl.net.impl.tftp;

public class IllegalTFTPOperationException extends RuntimeException {
    public IllegalTFTPOperationException(String message) {
        super(message);
    }

    public IllegalTFTPOperationException() {
        super();
    }

    public IllegalTFTPOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalTFTPOperationException(Throwable cause) {
        super(cause);
    }
}
