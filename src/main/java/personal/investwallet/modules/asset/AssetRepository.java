package personal.investwallet.modules.asset;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends MongoRepository<AssetEntity, String> {

    AssetEntity findByAssetName(String assetName);

}
