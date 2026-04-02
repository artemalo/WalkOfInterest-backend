package sfedu.ictis.woi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalServiceException extends RuntimeException {
    private static final Logger log = LoggerFactory.getLogger(ExternalServiceException.class);

    public ExternalServiceException(String serviceName, String message) {
        super(serviceName + ": " + message);
    }
    public ExternalServiceException(String serviceName, String message, Exception e) {
        super(serviceName + ": " + message, e);
    }
}
