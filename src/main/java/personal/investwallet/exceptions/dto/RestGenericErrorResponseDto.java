package personal.investwallet.exceptions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestGenericErrorResponseDto {

    private int code;
    private HttpStatus status;
    private String message;
}
