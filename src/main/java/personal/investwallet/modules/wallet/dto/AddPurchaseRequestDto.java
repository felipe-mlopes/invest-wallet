package personal.investwallet.modules.wallet.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record AddPurchaseRequestDto(
        @NotBlank(message = "O nome do ativo não pode ser vazio")
        @Size(min = 5, max = 6, message = "O nome do ativo deve conter entre 5 e 6 caracteres")
        String assetName,

        @Positive(message = "O número de cotas compradas deve ser maior que zero")
        int purchaseAmount,

        @Positive(message = "O preço da compra deve ser maior que zero")
        BigDecimal purchasePrice,

        @PastOrPresent(message = "A data da compra não pode ser no futuro")
        Instant purchaseDate
) {
}
