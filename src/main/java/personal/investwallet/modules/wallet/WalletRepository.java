package personal.investwallet.modules.wallet;

import org.bson.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<WalletEntity, String>, WalletRespositoryCustom {

    Optional<WalletEntity> findByUserId(String userId);

    @Query(value = "{ 'userId': ?0, 'assets.?1': { $exists: true } }",
            fields = "{ 'assets.?1.quota_amount': 1 }")
    Optional<Document> findQuotaAmountByUserIdAndAssetKey(String userId, String assetKey);
    
    @Query("{ 'userId': ?0 }")
    @Update("{ '$set': { 'assets.?1': ?2 } }")
    void addNewAssetByUserId(String userId, String assetName, WalletEntity.Asset newAsset);

    @Query("{ 'userId': ?0, 'assets.?1.assetName': ?1 }")
    @Update("{ '$inc': { 'assets.?1.quotaAmount': ?2 } }")
    void restoreAmountOfQuotasInAsset(String userId, String assetName, int previousPurchaseAmount);

    @Query("{ 'userId': ?0, 'assets.?1.assetName': ?1 }")
    @Update("{ '$set': { 'assets.?1.purchasesInfo': ?2 }, '$inc': { 'assets.?1.quotaAmount': ?3 } }")
    void updatePurchaseInAssetByPurchaseId(String userId, String assetName, List<WalletEntity.Asset.PurchasesInfo> purchasesInfo, int quotaDecrease);

    @Query("{ 'userId': ?0, 'assets.?1.assetName': ?1 }")
    @Update("{ '$set': { 'assets.?1.salesInfo': ?2 }, '$inc': { 'assets.?1.quotaAmount': ?3 } }")
    void updateSaleInAssetBySaleId(String userId, String assetName, List<WalletEntity.Asset.SalesInfo> salesInfo, int quotaIncrement);
}
