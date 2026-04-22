package sfedu.ictis.woi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Унифицированный ответ с ошибкой API")
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Schema(description = "Внутренний код ошибки", example = "UNAUTHORIZED")
    private final String code;

    @Schema(description = "Человекочитаемое сообщение", example = "Неверные учётные данные или токен")
    private final String message;

    @Schema(description = "Время возникновения ошибки", example = "2023-10-24T10:08:57")
    private final LocalDateTime timestamp;

    @Schema(description = "Детализированные ошибки (например, для полей при валидации)",
            example = "{\"email\": \"Неверный формат email\", \"password\": \"Слишком короткий пароль\"}")
    private final Map<String, String> details;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.details = null;
    }

    public ErrorResponse(String code, String message, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }
}