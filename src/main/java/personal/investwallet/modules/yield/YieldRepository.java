package personal.investwallet.modules.yield;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YieldRepository extends MongoRepository<YieldEntity, String> {

    boolean existsByUserAssetYieldAt(String userAssetYieldAt);

    @Query("{ 'user_id': ?0, 'yield_at': ?1 }")
    List<YieldEntity> findByUserIdAndYieldAt(String userId, String yieldAt);

    @Query("{ 'user_id': ?0, 'asset_name': ?1 }")
    List<YieldEntity> findByUserIdAndAssetName(String userId, String assetName);
}