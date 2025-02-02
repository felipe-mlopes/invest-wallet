package personal.investwallet.modules.asset;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends MongoRepository<AssetEntity, String> {

    Optional<AssetEntity> findByAssetName(String assetName);

    boolean existsByAssetName(String assetName);

    @Query(value = "{}", fields = "{ 'assetName' : 1, '_id' : 0}")
    Stream<String> findAllAssetNames();
}
