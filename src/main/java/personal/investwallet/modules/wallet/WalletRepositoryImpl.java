package personal.investwallet.modules.wallet;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class WalletRepositoryImpl implements WalletRespositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public WalletRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<String> findDistinctAssetNames() {

        Aggregation aggregation = newAggregation(
                project()
                        .andExpression("objectToArray(assets)")
                        .as("assetsArray"),
                unwind("assetsArray"),
                group("assetsArray.k")
                        .first("assetsArray.k")
                        .as("assetName"),
                project("assetName") );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                WalletEntity.class,
                Document.class
        );

        return results.getMappedResults().stream()
                .map(doc -> doc.getString("assetName"))
                .toList();
    }

    @Override
    public List<String> findUserIdsByAssetKey(String assetKey) {
        Aggregation aggregation = newAggregation(
                project("userId")
                        .andExpression("objectToArray(assets)")
                        .as("assetsArray"),
                match(Criteria.where("assetsArray.k").is(assetKey)),
                project("userId")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                WalletEntity.class,
                Document.class
        );

        return results.getMappedResults().stream()
                .map(doc -> doc.getString("userId"))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void addPurchaseToAsset(String userId, String assetName, WalletEntity.Asset.PurchasesInfo newPurchase, int quotaIncrement) {

        Query query = new Query(Criteria.where("user_id").is(userId).and("assets." + assetName).exists(true));

        Update update = new Update()
                .push("assets." + assetName + ".purchases_info", newPurchase)
                .inc("assets." + assetName + ".quota_amount", quotaIncrement);

        mongoTemplate.updateFirst(query, update, WalletEntity.class);
    }

    @Override
    public void addSaleToAsset(String userId, String assetName, WalletEntity.Asset.SalesInfo newSale, int quotaIncrement) {

        Query query = new Query(Criteria.where("user_id").is(userId).and("assets." + assetName).exists(true));

        Update update = new Update()
                .push("assets." + assetName + ".sales_info", newSale)
                .inc("assets." + assetName + ".quota_amount", quotaIncrement);

        mongoTemplate.updateFirst(query, update, WalletEntity.class);
    }
}
