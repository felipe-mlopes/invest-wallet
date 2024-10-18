package personal.investwallet.modules.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdatePurchaseRequestDto(
        Integer purchaseAmount,
        BigDecimal purchasePrice,
        Instant purchaseDate
) {
}
