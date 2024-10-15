package personal.investwallet.modules.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AddSaleRequestDto(String assetName, String assetType, int saleAmount, BigDecimal salePrice, Instant saleDate) {
}
