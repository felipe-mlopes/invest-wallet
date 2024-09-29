package personal.investwallet.exceptions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestValidationErrorResponseDto {

    private int code;
    private HttpStatus status;
    private List<String> errors;
}
