package sfedu.ictis.woi.exception;

public class PoiAlreadyExistsException extends BaseException {
    public PoiAlreadyExistsException(String message) {
        super(message, "POI_EXISTS");
    }
}
