package sfedu.ictis.woi.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {// TODO: testing
    Map<String, String> errors = new HashMap<>();

    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Неверные учётные данные или токен", "UNAUTHORIZED"));
  }

  @ExceptionHandler({DataAccessException.class, SQLException.class})
  public ResponseEntity<ErrorResponse> handleDb() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("База временно недоступна", "DB_ERROR"));
  }

  // 404 Not Found
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 401 Unauthorized
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(InvalidCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // 403 Forbidden
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }

  // Все остальные наследники BaseException (400 Bad Request)
  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
  }
}
