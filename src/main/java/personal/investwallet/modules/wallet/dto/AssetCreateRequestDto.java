package personal.investwallet.modules.wallet.dto;

import java.util.Set;

public record AssetCreateRequestDto(
        String assetName,
        String assetType,
        int quotaAmount,
        Set<PurchasesInfoRequestDto> purchasesInfo,
        Set<SalesInfoRequestDto> salesInfo
) {
}
