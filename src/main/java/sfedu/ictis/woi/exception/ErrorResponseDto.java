package sfedu.ictis.woi.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponseDto {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    public ErrorResponseDto(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

}
