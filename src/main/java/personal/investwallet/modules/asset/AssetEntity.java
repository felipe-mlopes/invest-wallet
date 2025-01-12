package personal.investwallet.modules.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "assets")
public class AssetEntity {

    @MongoId
    String id;

    @Indexed(unique = true)
    @Field("asset_name")
    String assetName;

    @Field("asset_type")
    String assetType;
}
