package sfedu.ictis.woi.exception;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException(String username) {
        super("Пользователь с именем " + username + " уже зарегистрирован", "USER_EXISTS");
    }
}
