package personal.investwallet.modules.wallet.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdatePurchaseRequestDto(
        @Positive(message = "O número de cotas compradas deve ser maior que zero")
        Integer purchaseAmount,

        @Positive(message = "O preço da compra deve ser maior que zero")
        BigDecimal purchasePrice,

        @PastOrPresent(message = "A data da compra não pode ser no futuro")
        Instant purchaseDate
) {
}
