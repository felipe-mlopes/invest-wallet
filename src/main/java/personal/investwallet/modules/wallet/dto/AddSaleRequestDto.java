package personal.investwallet.modules.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AddSaleRequestDto(
        @NotBlank(message = "O nome do ativo não pode ser vazio")
        @Size(min = 5, max = 6, message = "O nome do ativo deve conter entre 5 e 6 caracteres")
        String assetName,

        @Positive(message = "O número de cotas vendidas deve ser maior que zero")
        Integer saleAmount,

        @Positive(message = "O preço da venda deve ser maior que zero")
        BigDecimal salePrice,

        @PastOrPresent(message = "A data da venda não pode ser no futuro")
        Instant saleDate
) {
}
