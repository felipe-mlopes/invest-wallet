package personal.investwallet.modules.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InfoGenericDto(
        String id,
        int amount,
        BigDecimal price,
        BigDecimal quotaValue,
        Instant date
) {
}
