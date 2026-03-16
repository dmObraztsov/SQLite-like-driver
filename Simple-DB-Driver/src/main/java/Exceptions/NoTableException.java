package Exceptions;

public class NoTableException extends FileStorageException {
    public NoTableException(String message) {
        super(message);
    }

    public NoTableException(String message, Throwable cause) {
        super(message, cause);
    }
}
