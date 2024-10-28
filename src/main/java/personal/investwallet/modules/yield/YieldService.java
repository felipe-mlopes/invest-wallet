package personal.investwallet.modules.yield;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.modules.asset.AssetService;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;
import personal.investwallet.modules.yield.YieldEntity.YieldInfo;
import personal.investwallet.modules.yield.dto.YieldInfosResponseDto;
import personal.investwallet.modules.yield.dto.YieldInfoRequestDto;
import personal.investwallet.security.TokenService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class YieldService {

    @Autowired
    YieldRepository yieldRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    TokenService tokenService;

    @Autowired
    private ScraperService scraperService;

    @SneakyThrows
    public int registerAllYieldsReceivedInPreviousMonthsByFile(String token, MultipartFile file) {

        String userId = tokenService.extractUserIdFromToken(token);

        List<YieldInfoRequestDto> yields = readCSVFile(file);

        Map<String, Map<String, YieldInfo>> yieldByAssetName = new HashMap<>();
        Map<String, Map<String, YieldInfo>> yieldByPaymentAt = new HashMap<>();

        for (YieldInfoRequestDto yield : yields) {
            String assetName = yield.assetName();
            String yieldAt = yield.yieldAt();
            YieldInfo yieldInfo = yield.yieldInfo();

            yieldByAssetName
                    .computeIfAbsent(assetName, k -> new HashMap<>())
                    .put(yieldAt, yieldInfo);

            yieldByPaymentAt
                    .computeIfAbsent(yieldAt, k -> new HashMap<>())
                    .put(assetName, yieldInfo);
        }

        YieldInfosResponseDto yieldInfoResponseDto = new YieldInfosResponseDto(
                yieldByAssetName,
                yieldByPaymentAt
        );

        registerYield(userId, yieldInfoResponseDto);
        return yields.size();
    }

    public void registerAllYieldsReceivedInTheMonth(String token, List<Object> assets) {

        String userId = tokenService.extractUserIdFromToken(token);

        YieldInfosResponseDto yields = generateYieldInfo(assets);

        registerYield(userId, yields);

    }

    private static List<YieldInfoRequestDto> readCSVFile(MultipartFile file) throws IOException, CsvException {

        Reader reader = new InputStreamReader(file.getInputStream());

        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

        List<String[]> rows = csvReader.readAll();
        List<YieldInfoRequestDto> result = new ArrayList<>();

        // Pular o cabeçalho
        rows.remove(0);

        rows.forEach(row -> {
            for (int i = 0; i < row.length; i++)
                row[i] = row[i].replace(",", ".");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            String assetName = row[0];

            String yieldAt = row[1];

            String baseDate = row[2];
            LocalDate baseLocalDate = LocalDate.parse(baseDate, formatter);
            Instant baseDateInstant = baseLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            String paymentDate = row[3];
            LocalDate paymentLocalDate = LocalDate.parse(paymentDate, formatter);
            Instant paymentDateInstant = paymentLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            String basePrice = row[4];
            BigDecimal basePriceDecimal = new BigDecimal(basePrice.trim());

            String incomeValue = row[5];
            BigDecimal incomeValueDecimal = new BigDecimal(incomeValue.trim());

            String yieldValue = row[6];
            BigDecimal yieldValueDecimal = new BigDecimal(yieldValue.trim());

            YieldInfo yieldInfo = new YieldInfo(
                    baseDateInstant,
                    paymentDateInstant,
                    basePriceDecimal,
                    incomeValueDecimal,
                    yieldValueDecimal
            );

            YieldInfoRequestDto responseDto = new YieldInfoRequestDto(
                    assetName,
                    yieldAt,
                    yieldInfo
            );

            result.add(responseDto);

        });

        csvReader.close();

        return result;
    }

    private void registerYield(String userId, YieldInfosResponseDto yieldInfoResponseDto) {
        Optional<YieldEntity> existingYieldEntityOpt = yieldRepository.findByUserId(userId);

        if (existingYieldEntityOpt.isPresent()) {
            YieldEntity existingYieldEntity = existingYieldEntityOpt.get();

            extractedByAssetName(yieldInfoResponseDto, existingYieldEntity);
            extractedByPaymentAt(yieldInfoResponseDto, existingYieldEntity);

            yieldRepository.save(existingYieldEntity);

        } else {
            YieldEntity yieldEntity = new YieldEntity();
            yieldEntity.setUserId(userId);
            yieldEntity.getYieldByAssetName().putAll(yieldInfoResponseDto.yieldByAssetName());
            yieldEntity.getYieldByPaymentAt().putAll(yieldInfoResponseDto.yieldByPaymentAt());

            yieldRepository.save(yieldEntity);
        }
    }

    private static void extractedByAssetName(YieldInfosResponseDto yields, YieldEntity existingYieldEntity) {
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

    private static void extractedByPaymentAt(YieldInfosResponseDto yields, YieldEntity existingYieldEntity) {

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

    private YieldInfosResponseDto generateYieldInfo(List<Object> assets) {

        String todayFormattedAt = getYieldAt();

        Map<String, Map<String, YieldInfo>> yieldByAssetName = new HashMap<>();
        Map<String, Map<String, YieldInfo>> yieldByPaymentAt = new HashMap<>();

        for (Object asset : assets) {
            Map<String, Object> assetMap = (Map<String, Object>) asset;
            String assetName = (String) assetMap.get("assetName");
            int assetQuotaAmount = (int) assetMap.get("assetQuotaAmount");

            String assetType = assetService.getAssetTypeByAssetName(assetName);

            ScraperResponseDto values = scraperService.yieldScraping(assetType, assetName);

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

        return new YieldInfosResponseDto(yieldByAssetName, yieldByPaymentAt);
    }

    private static String getYieldAt() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter todayFormatted = DateTimeFormatter.ofPattern("yyyyMM");
        return today.format(todayFormatted);
    }

}
