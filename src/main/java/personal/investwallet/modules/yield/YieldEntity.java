package personal.investwallet.modules.yield;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "user_asset_idx", def = "{'userId': 1, 'yieldByAssetName.assetName': 1}"),
        @CompoundIndex(name = "yield_at_idx", def = "{'userId': 1, 'yieldByAssetName.assetName': 1, 'yield.yieldAt': 1}"),
})
@Document(collection = "yields")
public class YieldEntity {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("yield_by_asset_name")
    private Map<String, Map<String, YieldInfo>> yieldByAssetName = new HashMap<>();

    @Field("yield_by_payment_at")
    private Map<String, Map<String, YieldInfo>> yieldByPaymentAt = new HashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class YieldInfo {

        @Field("base_date")
        private Instant baseDate;

        @Field("payment_date")
        private Instant paymentDate;

        @Field("base_price")
        private BigDecimal basePrice;

        @Field("income_value")
        private BigDecimal incomeValue;

        @Field("yield_value")
        private BigDecimal yieldValue;
    }
}
