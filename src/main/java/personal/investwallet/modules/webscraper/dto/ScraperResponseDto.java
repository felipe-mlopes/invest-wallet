package personal.investwallet.modules.webscraper.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ScraperResponseDto(
        String assetName,
        BigDecimal incomeValue,
        BigDecimal basePrice,
        Instant basePriceDate,
        Instant basePaymentDate,
        String yieldAt
) {
}
