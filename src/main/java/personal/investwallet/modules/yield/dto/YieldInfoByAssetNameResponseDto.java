package personal.investwallet.modules.yield.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record YieldInfoByAssetNameResponseDto(
                String yieldAt,
                Instant baseDate,
                Instant paymentDate,
                BigDecimal basePrice,
                BigDecimal incomeValue,
                BigDecimal yieldValue) {
}