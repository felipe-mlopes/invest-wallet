package personal.investwallet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStringFormatException extends RuntimeException {

    public InvalidStringFormatException(String message) { super(message); }
}
