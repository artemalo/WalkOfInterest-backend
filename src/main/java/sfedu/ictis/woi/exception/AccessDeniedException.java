package sfedu.ictis.woi.exception;

public class AccessDeniedException extends BaseException {
  public AccessDeniedException(String message) {
    super(message, "ACCESS_DENIED");
  }
}
