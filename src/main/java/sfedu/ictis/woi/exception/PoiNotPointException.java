package sfedu.ictis.woi.exception;

public class PoiNotPointException extends BaseException {
    public PoiNotPointException(String message) {
        super(message, "BAD_POI");
    }
}
