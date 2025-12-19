package Exceptions;

public class NoDataBaseException extends FileManagerException {
    public NoDataBaseException(String message) {
        super(message);
    }

    public NoDataBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
