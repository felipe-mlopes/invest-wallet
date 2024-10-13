package personal.investwallet.modules.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    @Query("{ 'email': ?0 }")
    @Update("{ '$set': { 'is_checked': true, 'updated_at': ?1 } }")
    void updateCheckedAsTrueByEmail(String email, Instant updatedAt);
}
