package sfedu.ictis.woi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 400 Bad Request - Ошибки валидации DTO
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            "Ошибка валидации входных данных",
            errors
    );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<Object> handleAccessDenied(AuthorizationDeniedException ex) {
    log.warn("Попытка несанкционированного доступа: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCESS_DENIED", "У вас недостаточно прав для выполнения этой операции"));
  }

  // 401 Unauthorized - Ошибка секьюрити
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleAuthenticationException() {

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", "Неверные учётные данные или токен"));
  }

  // 503 Service Unavailable - Упала база
  @ExceptionHandler({DataAccessException.class, SQLException.class})
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ResponseEntity<ErrorResponse> handleDb(Exception ex) {
    log.error("Ошибка базы данных: ", ex);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("DB_ERROR", "База данных временно недоступна"));
  }

  // 404 Not Found
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 401 Unauthorized
  @ExceptionHandler({InvalidCredentialsException.class, BadTokenException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ResponseEntity<ErrorResponse> handleUnauthorized(BaseException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 403 Forbidden
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 409 Conflict
  @ExceptionHandler({UserAlreadyExistsException.class, PoiAlreadyExistsException.class})
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ErrorResponse> handleConflict(BaseException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 400 Bad Request - Все остальные
  @ExceptionHandler(BaseException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
    log.warn("Бизнес-ошибка: {} - {}", ex.getCode(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 500 Internal Server Error - Ошибка файлового хранилища
  @ExceptionHandler(FileStorageException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
    log.error("Ошибка файлового хранилища: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 500 Internal Server Error - Отлов всех непредвиденных ошибок (ОБЯЗАТЕЛЬНО!)
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
    log.error("Непредвиденная ошибка сервера: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Внутренняя ошибка сервера. Обратитесь к администратору."));
  }

  // 429 Too Many Requests - превышен лимит создания POI
  @ExceptionHandler(RateLimitExceededException.class)
  @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
  public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
    log.info("Rate limit exceeded: {}, retry after {}s", ex.getMessage(), ex.getRetryAfterSeconds());

    Map<String, String> details = new HashMap<>();
    details.put("retryAfterSeconds", String.valueOf(ex.getRetryAfterSeconds()));

    return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), details));
  }


  @ExceptionHandler({org.springframework.orm.jpa.JpaSystemException.class, org.hibernate.HibernateException.class})
  public ResponseEntity<Object> handleJpaException(RuntimeException ex) {
    log.error("Ошибка JPA/Hibernate: ", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Ошибка при чтении данных из базы. Проверьте корректность форматов."));
  }
}