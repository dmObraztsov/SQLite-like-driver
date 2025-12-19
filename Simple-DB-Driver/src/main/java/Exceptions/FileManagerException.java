package Exceptions;

public class FileManagerException extends Exception {
    public FileManagerException(String message) {
        super(message);
    }

    public FileManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}