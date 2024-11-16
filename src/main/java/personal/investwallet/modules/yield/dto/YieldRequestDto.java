package personal.investwallet.modules.yield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record YieldRequestDto(
        @NotBlank(message = "O nome do ativo não pode ser vazio")
        @Size(min = 5, max = 6, message = "O nome do ativo deve conter entre 5 e 6 caracteres")
        String assetName,

        @PastOrPresent(message = "A data base deve ser anterior a data corrente")
        Instant baseDate,

        @PastOrPresent(message = "A data de pagamento deve ser anterior a data corrente")
        Instant paymentDate,

        @Positive(message = "A cotação base deve ser maior que zero")
        BigDecimal basePrice,

        @Positive(message = "O valor de rendimento deve ser maior que zero")
        BigDecimal incomeValue,

        @Positive(message = "O valor do dividendo deve ser maior que zero")
        BigDecimal yieldValue
) {
}
