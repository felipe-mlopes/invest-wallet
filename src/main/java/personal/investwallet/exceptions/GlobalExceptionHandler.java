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

    @ExceptionHandler(value = ConflictException.class)
    public ResponseEntity<RestGenericErrorResponseDto> userAlreadyExistsException(ConflictException exception) {

        RestGenericErrorResponseDto response = new RestGenericErrorResponseDto(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<RestGenericErrorResponseDto> unauthorizedException(UnauthorizedException exception) {

        RestGenericErrorResponseDto response = new RestGenericErrorResponseDto(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED, exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(value = BadRequestException.class)
    public ResponseEntity<RestGenericErrorResponseDto> badRequestException(BadRequestException exception) {

        RestGenericErrorResponseDto response = new RestGenericErrorResponseDto(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestValidationErrorResponseDto> handleArgsNotValidExceptions(MethodArgumentNotValidException exception) {

        List<String> errorList = exception.getBindingResult().getFieldErrors().stream().map(
                error -> error.getField() + ": " + error.getDefaultMessage()
        ).toList();

        RestValidationErrorResponseDto response = new RestValidationErrorResponseDto(HttpStatus.BAD_REQUEST.value() ,HttpStatus.BAD_REQUEST, errorList);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
