package personal.investwallet.modules.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssetInfoDto(
                @NotBlank(message = "O nome do ativo não pode ser vazio") @Size(min = 5, max = 6, message = "O nome do ativo deve conter entre 5 e 6 caracteres") String assetName,

                @NotBlank(message = "O nome do ativo não pode ser vazio") @Pattern(regexp = "acoes|fundos-imobiliarios", message = "O tipo do ativo deve ser 'acoes' ou 'fundos-imobiliarios'") String assetType) {
}
