package personal.investwallet.modules.user.dto;

import jakarta.validation.constraints.*;

public record UserLoginRequestDto(
                @NotBlank(message = "O e-mail não pode ser vazio") @Email(message = "Formato inválido de email") String email,

                @NotBlank(message = "A senha não pode ser vazia") String password) {
}
