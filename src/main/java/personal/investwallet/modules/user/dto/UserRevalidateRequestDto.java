package personal.investwallet.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRevalidateRequestDto(
        @NotBlank(message = "O e-mail não pode ser vazio") @Email(message = "Formato inválido de email") String email
) {
}
