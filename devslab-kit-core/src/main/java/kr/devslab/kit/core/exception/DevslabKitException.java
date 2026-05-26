package kr.devslab.kit.core.exception;

public class DevslabKitException extends RuntimeException {

    public DevslabKitException(String message) {
        super(message);
    }

    public DevslabKitException(String message, Throwable cause) {
        super(message, cause);
    }
}
