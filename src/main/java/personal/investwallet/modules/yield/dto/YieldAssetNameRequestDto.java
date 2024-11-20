package personal.investwallet.modules.yield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record YieldAssetNameRequestDto(
        @NotBlank(message = "O nome do ativo não pode ser vazio") @Size(min = 5, max = 6, message = "O nome do ativo deve conter entre 5 e 6 caracteres") String assetName) {
}
