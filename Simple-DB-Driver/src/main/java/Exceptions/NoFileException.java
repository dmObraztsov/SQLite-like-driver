package Exceptions;

public class NoFileException extends FileStorageException {
    public NoFileException(String message) {
        super(message);
    }

    public NoFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
