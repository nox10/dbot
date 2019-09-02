package xyz.dbotfactory.dbot;

public class DBotUserException extends RuntimeException {
    public DBotUserException() {

    }

    public DBotUserException(String message) {
        super(message);
    }

    public DBotUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBotUserException(Throwable cause) {
        super(cause);
    }

    public DBotUserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
