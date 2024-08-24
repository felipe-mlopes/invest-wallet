package personal.investwallet.modules.user.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequestDto(
                @NotBlank(message = "O nome não pode ser vazio") @Size(min = 2, max = 30, message = "O nome deve ter entre 2 e 30 caracteres") String name,

                @NotBlank(message = "O e-mail não pode ser vazio") @Email(message = "Formato inválido de email") String email,

                @NotBlank(message = "A senha não pode ser vazia") @Size(min = 6, max = 12, message = "A senha deve ter entre 6 e 12 caracteres") String password) {
}
