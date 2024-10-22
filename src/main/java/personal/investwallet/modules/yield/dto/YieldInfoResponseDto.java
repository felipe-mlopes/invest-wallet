package personal.investwallet.modules.yield.dto;

import personal.investwallet.modules.yield.YieldEntity;

import java.util.Map;

public record YieldInfoResponseDto(
        Map<String, Map<String, YieldEntity.YieldInfo>> yieldByAssetName,
        Map<String, Map<String, YieldEntity.YieldInfo>> yieldByPaymentAt
) {
}
