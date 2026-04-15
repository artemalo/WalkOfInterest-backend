package sfedu.ictis.woi.exception;

public class ResourceNotFoundException extends BaseException {
  public ResourceNotFoundException(String resourceName, Object identifier) {
    super(String.format("%s с идентификатором %s не найден", resourceName, identifier),
            "RESOURCE_NOT_FOUND");
  }
}
