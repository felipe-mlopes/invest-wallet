package personal.investwallet.modules.wallet;

import java.util.List;

public interface WalletRespositoryCustom {

    List<String> findDistinctAssetNames();

    List<String> findUserIdsByAssetKey(String assetKey);

    void addPurchaseToAsset(String userId, String assetName, WalletEntity.Asset.PurchasesInfo newPurchase, int quotaIncrement);

    void addSaleToAsset(String userId, String assetName, WalletEntity.Asset.SalesInfo newSale, int quotaIncrement);
}
