package personal.investwallet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) { super(message); }
}
