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
import personal.investwallet.exceptions.*;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

        validateFile(file);

        List<YieldInfoRequestDto> yields = readCSVFile(file);

        Map<String, Map<String, YieldInfo>> yieldByAssetName = new HashMap<>();
        Map<String, Map<String, YieldInfo>> yieldByPaymentAt = new HashMap<>();

        for (YieldInfoRequestDto yield : yields) {
            String assetName = yield.assetName();

            String assetType = assetService.getAssetTypeByAssetName(assetName);

            if (assetType == null)
                throw new ResourceNotFoundException("O ativo " + assetName + " informado não existe.");

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

    private static List<YieldInfoRequestDto> readCSVFile(MultipartFile file) {

        try {
            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

            List<String[]> rows = csvReader.readAll();
            if (rows.size() <= 1)
                throw new EmptyFileException("Arquivo é inválido por estar vazio ou com apenas o cabeçalho preenchido");

            validateHeader(rows.get(0));

            return processRows(rows.subList(1, rows.size()));

        } catch (IOException e) {
            throw new FileProcessingException("Erro ao ler o arquivo CSV");
        } catch (CsvException e) {
            throw new InvalidFileFormatException("Erro ao processar o arquivo CSV");
        }
    }

    private static List<YieldInfoRequestDto> processRows(List<String[]> rows) {
        List<YieldInfoRequestDto> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            try {
                String[] row = rows.get(rowNum);
                for (int i = 0; i < row.length; i++) {
                    row[i] = row[i].replace(",", ".");
                }

                validateRowData(row, rowNum + 2); // +2 porque começamos após o cabeçalho

                String assetName = row[0].trim();
                String yieldAt = row[1].trim();

                validateYieldAt(yieldAt);

                LocalDate baseLocalDate = parseDate(row[2], formatter, "Data Base", rowNum + 2);
                LocalDate paymentLocalDate = parseDate(row[3], formatter, "Data de Pagamento", rowNum + 2);

                validateDate(baseLocalDate, paymentLocalDate);

                BigDecimal basePrice = parseBigDecimal(row[4], "Preço Base", rowNum + 2);
                BigDecimal incomeValue = parseBigDecimal(row[5], "Valor do Rendimento", rowNum + 2);
                BigDecimal yieldValue = parseBigDecimal(row[6], "Valor do Yield", rowNum + 2);

                YieldInfo yieldInfo = new YieldInfo(
                        baseLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        paymentLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        basePrice,
                        incomeValue,
                        yieldValue
                );

                result.add(new YieldInfoRequestDto(assetName, yieldAt, yieldInfo));

            } catch (DateTimeParseException e) {
                throw new InvalidDateFormatException(
                        "Erro na linha " + (rowNum + 2) + ": " + e.getMessage()
                );
            } catch (NumberFormatException e) {
                throw new InvalidNumberFormatException(
                        "Erro na linha " + (rowNum + 2) + ": " + e.getMessage()
                );
            }
        }

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

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("O arquivo não enviado ou não preenchido");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileFormatException("O arquivo deve ser um CSV válido");
        }
    }

    private static void validateHeader(String[] header) {

        String[] expectedHeaders = {
                "Asset Name", "Yield At", "Base Date", "Payment Date",
                "Base Price", "Income Value", "Yield Value"
        };

        if (header.length != expectedHeaders.length) {
            throw new InvalidFileFormatException(
                    "Formato de cabeçalho inválido. Esperado: " + String.join(", ", expectedHeaders)
            );
        }

        for (int i = 0; i < expectedHeaders.length; i++) {
            if (!header[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                throw new InvalidFileFormatException(
                        "Coluna inválida no cabeçalho. Esperado: '" + expectedHeaders[i] +
                                "', Encontrado: '" + header[i].trim() + "'"
                );
            }
        }
    }

    private static void validateRowData(String[] row, int rowNum) {

        if (row.length != 7)
            throw new InvalidFileFormatException(
                    "A linha " + rowNum + " possui número incorreto de colunas"
            );


        for (int i = 0; i < row.length; i++) {
            if (row[i] == null || row[i].trim().isEmpty()) {
                throw new InvalidFileFormatException(
                        "Na linha " + rowNum + ", a coluna " + (i + 1) + " está vazia"
                );
            }
        }
    }

    private static void validateDate(LocalDate baseDate, LocalDate paymentDate) {

        if (paymentDate.isBefore(baseDate))
            throw new InvalidDateFormatException(
                    "A data de pagamento precisa ser maior que a data base de cálculo do dividendo"
            );

        int currentYear = LocalDate.now().getYear();

        if (baseDate.getYear() > currentYear || paymentDate.getYear() > currentYear)
            throw new InvalidDateFormatException(
                    "O ano da data base e/ou da data de pagamento precisa ser menor ou igual a ano corrente"
            );
    }

    private static void validateYieldAt(String yieldAt) {

        if (yieldAt.length() != 6)
            throw new InvalidStringFormatException(
                    "O yieldAt deve conter apenas 6 caracteres contendo o ano (yyyy) e o mês (mm)"
            );

        int currentYear = LocalDate.now().getYear();

        for (int i = 0; i < yieldAt.length(); i++) {
            int year = Integer.parseInt(yieldAt.substring(0, 4));
            int month = Integer.parseInt(yieldAt.substring(4));

            if (year > currentYear)
                throw new InvalidStringFormatException(
                        "O ano informado no YieldAt deve ter 4 caracteres e ser menor ou igual ao ano corrente"
                );

            if (month > 12)
                throw new InvalidStringFormatException(
                        "O mês informado no YieldAt deve ter 2 caracteres e ser válido"
                );
        }
    }

    private static LocalDate parseDate(String dateStr, DateTimeFormatter formatter,
                                       String fieldName, int rowNum) {

        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(
                    "Erro na linha " + rowNum + ", " + fieldName +
                            ": formato de data inválido. Use dd/MM/yyyy"
            );
        }
    }

    private static BigDecimal parseBigDecimal(String value, String fieldName, int rowNum) {

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidNumberFormatException(
                    "Erro na linha " + rowNum + ", " + fieldName +
                            ": valor numérico inválido"
            );
        }
    }

}
