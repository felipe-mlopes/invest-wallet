package personal.investwallet.modules.yield;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "yields")
public class YieldEntity {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Indexed(useGeneratedName = true)
    @Field("user_id")
    private String userId;

    @Field("asset_name")
    private String assetName;

    @Indexed(useGeneratedName = true)
    @Field("yield_at")
    private String yieldAt;

    @Indexed(unique = true)
    @Field("user_asset_yield_at")
    private String userAssetYieldAt;

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
