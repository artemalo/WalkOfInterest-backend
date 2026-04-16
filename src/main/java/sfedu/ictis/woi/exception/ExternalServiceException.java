package sfedu.ictis.woi.exception;


public class ExternalServiceException extends BaseException {
    public ExternalServiceException(String serviceName, String message) {
        super(serviceName + ": " + message, "SERVICE_ERROR");
    }
}
