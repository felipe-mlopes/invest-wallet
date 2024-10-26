package personal.investwallet.modules.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
@Document(collection = "wallets")
@CompoundIndexes({
        @CompoundIndex(name = "user_asset_idx", def = "{'userId': 1, 'asset.assetName': 1}"),
        @CompoundIndex(name = "purchase_idx", def = "{'asset.purchasesInfo.purchaseId': 1}"),
        @CompoundIndex(name = "sale_idx", def = "{'asset.salesInfo.saleId': 1}")
})
public class WalletEntity {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    private Map<String, Asset> assets = new HashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Asset {

        @Field("asset_name")
        private String assetName;

        @Field("quota_amount")
        private int quotaAmount;

        @Field("purchases_info")
        private List<PurchasesInfo> purchasesInfo;

        @Field("sales_info")
        private List<SalesInfo> salesInfo;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PurchasesInfo {

            @Id
            @Field("purchase_id")
            private String purchaseId;

            @Field("purchase_amount")
            private int purchaseAmount;

            @Field("purchase_price")
            private BigDecimal purchasePrice;

            @Field("purchase_date")
            private Instant purchaseDate;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SalesInfo {

            @Id
            @Field("sale_id")
            private String saleId;

            @Field("sale_amount")
            private int saleAmount;

            @Field("sale_price")
            private BigDecimal salePrice;

            @Field("sale_date")
            private Instant saleDate;
        }
    }
}
