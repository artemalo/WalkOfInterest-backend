package sfedu.ictis.woi.exception;


public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException() {
        super("Неверная почта или пароль", "INVALID_CREDENTIALS");
    }
}