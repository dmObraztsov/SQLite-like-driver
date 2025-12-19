package Exceptions;

public class FileTypeException extends FileStorageException {
    public FileTypeException(String message) {
        super(message);
    }

    public FileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}