package Exceptions;

public class SerializationStorageException extends FileStorageException {
    public SerializationStorageException(String message) {
        super(message);
    }

    public SerializationStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}