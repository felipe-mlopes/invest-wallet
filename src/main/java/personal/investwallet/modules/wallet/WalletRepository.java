package personal.investwallet.modules.wallet;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<WalletEntity, String> {

    Optional<WalletEntity> findByUserId(String userId);

    @Query("{ 'userId': ?0, 'asset.?1.assetName': ?1 }")
    @Update("{ '$push': { 'asset.?1.purchasesInfo': ?2 }, '$inc': { 'asset.?1.quotaAmount': ?3 } }")
    void updatePurchasesInfo(String userId, String assetName, WalletEntity.Asset.PurchasesInfo purchasesInfo, int quotaIncrement);
}
