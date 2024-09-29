package personal.investwallet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import personal.investwallet.exceptions.dto.RestGenericErrorResponseDto;
import personal.investwallet.exceptions.dto.RestValidationErrorResponseDto;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<RestGenericErrorResponseDto> recordNotFoundException(ResourceNotFoundException exception) {

        RestGenericErrorResponseDto response = new RestGenericErrorResponseDto(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND, exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(value = UserAlreadyExistsException.class)
    public ResponseEntity<RestGenericErrorResponseDto> userAlreadyExistsException(UserAlreadyExistsException exception) {

        RestGenericErrorResponseDto response = new RestGenericErrorResponseDto(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestValidationErrorResponseDto> handleArgsNotValidExceptions(MethodArgumentNotValidException exception) {

        List<String> errorList = exception.getBindingResult().getFieldErrors().stream().map(
                error -> error.getField() + ": " + error.getDefaultMessage()
        ).toList();

        RestValidationErrorResponseDto response = new RestValidationErrorResponseDto(HttpStatus.FORBIDDEN.value() ,HttpStatus.FORBIDDEN, errorList);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}