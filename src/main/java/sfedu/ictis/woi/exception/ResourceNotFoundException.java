package sfedu.ictis.woi.exception;

public class ResourceNotFoundException extends BaseException {
  public ResourceNotFoundException(String msg) {
    super(msg, "RESOURCE_NOT_FOUND");
  }
}
