package sfedu.ictis.woi.exception;


public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException() {
        super("Неверное имя пользователя или пароль", "INVALID_CREDENTIALS");
    }
}