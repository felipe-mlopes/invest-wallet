package personal.investwallet.modules.user.dto;

import jakarta.validation.constraints.*;

public record UserValidateRequestDto(
        @NotBlank(message = "O e-mail não pode ser vazio")
        @Email(message = "Formato inválido de email")
        String email,

        @NotBlank(message = "O código de verificação não pode ser vazio")
        @Pattern(regexp = "[A-Za-z0-9]{4}", message = "O código de verificação deve conter 4 dígitos")
        String code
) {
}
