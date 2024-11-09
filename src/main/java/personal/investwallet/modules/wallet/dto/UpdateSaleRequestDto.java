package personal.investwallet.modules.wallet.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateSaleRequestDto(
        @Positive(message = "O número de cotas vendidas deve ser maior que zero")
        Integer saleAmount,

        @Positive(message = "O preço da venda deve ser maior que zero")
        BigDecimal salePrice,

        @PastOrPresent(message = "A data da venda não pode ser no futuro")
        Instant saleDate
) {
}
