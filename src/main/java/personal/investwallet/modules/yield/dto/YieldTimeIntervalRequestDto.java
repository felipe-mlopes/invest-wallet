package personal.investwallet.modules.yield.dto;

import java.time.Instant;

public record YieldTimeIntervalRequestDto(
        Instant startAt,
        Instant endAt
) {
}
