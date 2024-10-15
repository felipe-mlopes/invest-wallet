package personal.investwallet.modules.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CompoundIndex(def = "{ userId: 1, assetName: 1 }", name = "idx0")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "wallets")
public class WalletEntity {

    @Indexed(unique = true)
    private String userId;

    private Map<String, Asset> asset = new HashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Asset {

        @Indexed(unique = true)
        private String assetName;
        private int quotaAmount;

        private List<PurchasesInfo> purchasesInfo;

        private List<SalesInfo> salesInfo;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PurchasesInfo {

            @Id
            private String purchaseId;
            private int purchaseAmount;
            private BigDecimal purchasePrice;
            private Instant purchaseDate;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SalesInfo {

            @Id
            private String saleId;
            private int saleAmount;
            private BigDecimal salePrice;
            private Instant saleDate;
        }
    }
}
