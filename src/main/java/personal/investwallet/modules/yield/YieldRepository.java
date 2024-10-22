package personal.investwallet.modules.yield;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YieldRepository extends MongoRepository<YieldEntity, String> {

    Optional<YieldEntity> findByUserId(String userId);

}
