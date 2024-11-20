package personal.investwallet.modules.yield.dto;

import java.time.Instant;

import jakarta.validation.constraints.PastOrPresent;

public record YieldTimeIntervalRequestDto(
                @PastOrPresent(message = "A data de início deve ser anterior a data corrente") Instant startAt,

                @PastOrPresent(message = "A data de término deve ser anterior a data corrente") Instant endAt) {
}
