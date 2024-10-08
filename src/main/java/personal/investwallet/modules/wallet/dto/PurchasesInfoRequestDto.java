package personal.investwallet.modules.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchasesInfoRequestDto(String assetName, String assetType, int purchaseAmount, BigDecimal purchasePrice, Instant purchaseDate) {
}
