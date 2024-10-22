package personal.investwallet.modules.yield;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import personal.investwallet.modules.wallet.WalletService;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;
import personal.investwallet.modules.yield.YieldEntity.YieldInfo;
import personal.investwallet.modules.yield.dto.YieldInfoResponseDto;
import personal.investwallet.security.TokenService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class YieldService {

    @Autowired
    YieldRepository yieldRepository;

    @Autowired
    WalletService walletService;

    @Autowired
    TokenService tokenService;

    @Autowired
    private ScraperService scraperService;

    private final String ASSET_TYPE = "fundos-imobiliarios";

    public void registerAllYieldsReceivedInTheMonth(String token, List<Object> assets) {

        String userId = tokenService.extractUserIdFromToken(token);

        YieldInfoResponseDto yields = generateYieldInfo(assets);

        Optional<YieldEntity> existingYieldEntityOpt = yieldRepository.findByUserId(userId);

        if (existingYieldEntityOpt.isPresent()) {
            YieldEntity existingYieldEntity = existingYieldEntityOpt.get();

            extractedByAssetName(yields, existingYieldEntity);
            extractedByPaymentAt(yields, existingYieldEntity);

            yieldRepository.save(existingYieldEntity);

        } else {
            YieldEntity yieldEntity = new YieldEntity();
            yieldEntity.setUserId(userId);
            yieldEntity.getYieldByAssetName().putAll(yields.yieldByAssetName());
            yieldEntity.getYieldByPaymentAt().putAll(yields.yieldByPaymentAt());

            yieldRepository.save(yieldEntity);
        }

    }

    private static void extractedByAssetName(YieldInfoResponseDto yields, YieldEntity existingYieldEntity) {
        for (Map.Entry<String, Map<String, YieldInfo>> newYieldEntry : yields.yieldByAssetName().entrySet()) {
            String assetName = newYieldEntry.getKey();
            Map<String, YieldInfo> yieldInfoMap = newYieldEntry.getValue();

            Map<String, YieldInfo> existingYieldsForAsset = existingYieldEntity.getYieldByAssetName()
                    .computeIfAbsent(assetName, k -> new HashMap<>());

            for (Map.Entry<String, YieldInfo> yieldInfoEntry : yieldInfoMap.entrySet()) {
                String yieldAt = yieldInfoEntry.getKey();
                YieldInfo yieldInfo = yieldInfoEntry.getValue();

                if (!existingYieldsForAsset.containsKey(yieldAt)) {
                    existingYieldsForAsset.put(yieldAt, yieldInfo);
                }
            }
        }
    }

    private static void extractedByPaymentAt(YieldInfoResponseDto yields, YieldEntity existingYieldEntity) {

        for (Map.Entry<String, Map<String, YieldInfo>> paymentEntry : yields.yieldByPaymentAt().entrySet()) {
            String paymentAt = paymentEntry.getKey();
            Map<String, YieldInfo> yieldInfoMap = paymentEntry.getValue();

            Map<String, YieldInfo> yieldsAlreadySave = existingYieldEntity.getYieldByPaymentAt()
                    .computeIfAbsent(paymentAt, k -> new HashMap<>());

            for (Map.Entry<String, YieldInfo> assetEntry : yieldInfoMap.entrySet()) {
                String asset = assetEntry.getKey();
                YieldInfo newYieldInfo = assetEntry.getValue();

                if (!yieldsAlreadySave.containsKey(asset)) {
                    yieldsAlreadySave.put(asset, newYieldInfo);
                }
            }
        }

    }

    private YieldInfoResponseDto generateYieldInfo(List<Object> assets) {

        String todayFormattedAt = getYieldAt();

        Map<String, Map<String, YieldInfo>> yieldByAssetName = new HashMap<>();
        Map<String, Map<String, YieldInfo>> yieldByPaymentAt = new HashMap<>();

        for (Object asset : assets) {
            Map<String, Object> assetMap = (Map<String, Object>) asset;
            String assetName = (String) assetMap.get("assetName");
            int assetQuotaAmount = (int) assetMap.get("assetQuotaAmount");

            ScraperResponseDto values = scraperService.scrapeWebsite(ASSET_TYPE, assetName);

            BigDecimal assetQuotaAmountBigDecimal = BigDecimal.valueOf(assetQuotaAmount);
            BigDecimal incomeValue = assetQuotaAmountBigDecimal.multiply(values.yieldValue());

            if (values.yieldAt().equals(todayFormattedAt)) {
                Map<String, YieldInfo> yieldInfoListByAssetName = new HashMap<>();
                yieldInfoListByAssetName.put(values.yieldAt(), new YieldInfo(
                        values.basePriceDate(),
                        values.basePaymentDate(),
                        values.basePrice(),
                        incomeValue,
                        values.yieldValue()
                ));
                yieldByAssetName.put(assetName, yieldInfoListByAssetName);

                Map<String, YieldInfo> yieldInfoListByPayment = yieldByPaymentAt
                        .computeIfAbsent(values.yieldAt(), k -> new HashMap<>());

                yieldInfoListByPayment.put(assetName, new YieldInfo(
                        values.basePriceDate(),
                        values.basePaymentDate(),
                        values.basePrice(),
                        incomeValue,
                        values.yieldValue()
                ));
            }
        }

        return new YieldInfoResponseDto(yieldByAssetName, yieldByPaymentAt);
    }

    private static String getYieldAt() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter todayFormatted = DateTimeFormatter.ofPattern("yyyyMM");
        return today.format(todayFormatted);
    }

}
