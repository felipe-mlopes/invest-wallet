package personal.investwallet.modules.yield.dto;

import personal.investwallet.modules.yield.YieldEntity;

public record YieldInfoRequestDto(
        String assetName,
        String yieldAt,
        YieldEntity.YieldInfo yieldInfo
) {
}
