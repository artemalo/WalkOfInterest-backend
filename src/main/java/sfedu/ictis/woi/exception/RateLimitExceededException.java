package sfedu.ictis.woi.exception;

import lombok.Getter;

/**
 * Бросается когда пользователь превысил лимит создания POI
 */
@Getter
public class RateLimitExceededException extends BaseException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message, "RATE_LIMIT_EXCEEDED");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}