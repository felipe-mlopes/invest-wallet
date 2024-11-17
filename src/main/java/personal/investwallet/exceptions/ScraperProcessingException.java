package personal.investwallet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ScraperProcessingException extends RuntimeException {

    public ScraperProcessingException(String message) { super(message); }
}
