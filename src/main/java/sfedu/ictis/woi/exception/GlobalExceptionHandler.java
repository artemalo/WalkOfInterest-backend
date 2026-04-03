package sfedu.ictis.woi.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponseDto> handleDb() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponseDto("DB_DOWN", "База временно недоступна"));
  }
  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponseDto> handleMicroserviceException() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponseDto("SERVICE_DOWN", "Сервис временно недоступен"));
  }


  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponseDto> handleServiceException() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponseDto("BUS_ERR", "Сервис не важно себя почувствовал"));
  }
}
