package sfedu.ictis.woi.exception;


public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException() {
        super("Пользователь не авторизован", "INVALID_CREDENTIALS");
    }
}