package personal.investwallet.modules.yield.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record YieldInfoByYieldAtResponseDto(
        String assetName,
        Instant baseDate,
        Instant paymentDate,
        BigDecimal basePrice,
        BigDecimal incomeValue,
        BigDecimal yieldValue) {
}
