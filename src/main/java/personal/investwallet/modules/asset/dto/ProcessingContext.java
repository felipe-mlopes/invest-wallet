package personal.investwallet.modules.asset.dto;

import java.util.List;
import java.util.Set;

import personal.investwallet.modules.asset.AssetEntity;

public record ProcessingContext(
        List<AssetEntity> assetsToSave,
        Set<String> processedNames,
        Set<String> existingNames) {
}