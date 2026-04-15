package sfedu.ictis.woi.exception;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message, "SERVICE_ERROR");
    }
}
