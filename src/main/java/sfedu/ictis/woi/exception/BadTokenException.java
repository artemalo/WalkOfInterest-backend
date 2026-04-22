package sfedu.ictis.woi.exception;

public class BadTokenException extends BaseException {
    public BadTokenException(String message) {
        super(message, "TOKEN_ERROR");
    }
}
