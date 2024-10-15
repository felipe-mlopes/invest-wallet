package personal.investwallet.modules.wallet.dto;

public record CreateAssetRequestDto(
        String assetName,
        String assetType,
        int quotaAmount
) {
}
