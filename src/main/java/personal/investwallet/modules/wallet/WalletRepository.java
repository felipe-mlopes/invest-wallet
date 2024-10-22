package personal.investwallet.modules.wallet;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<WalletEntity, String> {

    boolean existsByUserId(String userId);

    Optional<WalletEntity> findByUserId(String userId);
    
    @Query("{ 'userId': ?0 }")
    @Update("{ '$set': { 'asset.?1': ?2 } }")
    void addNewAssetByUserId(String userId, String assetName, WalletEntity.Asset newAsset);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$push': { 'asset.?1.purchasesInfo': ?2 }, '$inc': { 'asset.?1.quotaAmount': ?3 } }")
    void addPurchaseToAssetByUserIdAndAssetName(String userId, String assetName, WalletEntity.Asset.PurchasesInfo purchasesInfo, int quotaIncrement);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$push': { 'asset.?1.salesInfo': ?2 }, '$inc': { 'asset.?1.quotaAmount': ?3 } }")
    void addSaleToAssetByUserIdAndAssetName(String userId, String assetName, WalletEntity.Asset.SalesInfo salesInfo, int quotaDecrease);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$inc': { 'asset.?1.quotaAmount': ?2 } }")
    void restoreAmountOfQuotasInAsset(String userId, String assetName, int previousPurchaseAmount);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$set': { 'asset.?1.purchasesInfo': ?2 }, '$inc': { 'asset.?1.quotaAmount': ?3 } }")
    void updatePurchaseInAssetByPurchaseId(String userId, String assetName, List<WalletEntity.Asset.PurchasesInfo> purchasesInfo, int quotaDecrease);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$set': { 'asset.?1.salesInfo': ?2 }, '$inc': { 'asset.?1.quotaAmount': ?3 } }")
    void updateSaleInAssetBySaleId(String userId, String assetName, List<WalletEntity.Asset.SalesInfo> salesInfo, int quotaIncrement);
}
