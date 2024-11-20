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
import personal.investwallet.modules.wallet.WalletService;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.modules.webscraper.dto.ScraperResponseDto;
import personal.investwallet.modules.yield.dto.YieldAssetNameRequestDto;
import personal.investwallet.modules.yield.dto.YieldInfoByAssetNameResponseDto;
import personal.investwallet.modules.yield.dto.YieldInfoByYieldAtResponseDto;
import personal.investwallet.modules.yield.dto.YieldRequestDto;
import personal.investwallet.modules.yield.dto.YieldTimeIntervalRequestDto;
import personal.investwallet.security.TokenService;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
    WalletService walletService;

    @Autowired
    ScraperService scraperService;

    @Autowired
    TokenService tokenService;

    public Map<String, List<YieldInfoByYieldAtResponseDto>> fetchAllYieldsByTimeInterval(String token,
            YieldTimeIntervalRequestDto payload) {

        String userId = tokenService.extractUserIdFromToken(token);

        List<YieldEntity> yieldList = new ArrayList<>();
        List<String> yields = getYieldsAt(payload);

        for (String yield : yields) {
            List<YieldEntity> result = yieldRepository.findByUserIdAndYieldAt(userId, yield);
            yieldList.addAll(result);
        }

        Map<String, List<YieldInfoByYieldAtResponseDto>> resultMap = new HashMap<>();

        for (YieldEntity entity : yieldList) {
            resultMap
                    .computeIfAbsent(entity.getYieldAt(), k -> new ArrayList<>())
                    .add(new YieldInfoByYieldAtResponseDto(
                            entity.getAssetName(),
                            entity.getBaseDate(),
                            entity.getPaymentDate(),
                            entity.getBasePrice(),
                            entity.getIncomeValue(),
                            entity.getYieldValue()));
        }

        return resultMap;
    }

    public Map<String, List<YieldInfoByAssetNameResponseDto>> fetchAllYieldAtByAssetName(String token,
            YieldAssetNameRequestDto payload) {

        String userId = tokenService.extractUserIdFromToken(token);

        List<YieldEntity> yield = yieldRepository.findByUserIdAndAssetName(userId, payload.assetName());

        Map<String, List<YieldInfoByAssetNameResponseDto>> resultMap = new HashMap<>();

        for (YieldEntity entity : yield) {
            resultMap
                    .computeIfAbsent(entity.getAssetName(), k -> new ArrayList<>())
                    .add(new YieldInfoByAssetNameResponseDto(
                            entity.getYieldAt(),
                            entity.getBaseDate(),
                            entity.getPaymentDate(),
                            entity.getBasePrice(),
                            entity.getIncomeValue(),
                            entity.getYieldValue()));
        }

        return resultMap;
    }

    public int registerManyYieldsReceived(String token, List<YieldRequestDto> yields) {

        String userId = tokenService.extractUserIdFromToken(token);

        List<YieldEntity> yieldList = getYieldEntities(yields, userId);

        yieldRepository.saveAll(yieldList);

        return yields.size();
    }

    @SneakyThrows
    public int registerManyYieldsReceivedByCsv(String token, MultipartFile file) {

        String userId = tokenService.extractUserIdFromToken(token);

        validateFile(file);

        List<YieldRequestDto> yields = readCSVFile(file);
        List<YieldEntity> yieldList = getYieldEntities(yields, userId);

        yieldRepository.saveAll(yieldList);

        return yields.size();
    }

    public void registerManyFIIYieldsReceivedInCurrentMonthByWebScraping() {

        List<String> assetNames = walletService.getAllAssetNames();
        String yieldCorrentAt = generateYieldAt(Instant.now());

        List<YieldEntity> yieldList = new ArrayList<>();

        for (String assetName : assetNames) {
            String assetType = assetService.getAssetTypeByAssetName(assetName);
            List<String> userIds = walletService.getAllUserIdsWithWalletCreatedByAssetName(assetName);

            if (assetType.equals("fundos-imobiliarios")) {
                ScraperResponseDto scraper = scraperService.fiiYieldScraping(assetType, assetName);

                if (scraper.yieldAt().equals(yieldCorrentAt)) {
                    for (String userId : userIds) {
                        String userAssetYieldAt = userId + assetName + yieldCorrentAt;

                        if (!yieldRepository.existsByUserAssetYieldAt(userAssetYieldAt)) {
                            Integer quotaAmount = walletService.getQuotaAmountOfAssetByUserId(userId, assetName);

                            if (quotaAmount != null && quotaAmount > 0) {
                                BigDecimal yieldValue = scraper.incomeValue().multiply(BigDecimal.valueOf(quotaAmount));

                                yieldList.add(new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        assetName,
                                        yieldCorrentAt,
                                        userAssetYieldAt,
                                        scraper.basePriceDate(),
                                        scraper.basePaymentDate(),
                                        scraper.basePrice(),
                                        scraper.incomeValue(),
                                        yieldValue));
                            }
                        }
                    }
                }
            }
        }

        yieldRepository.saveAll(yieldList);
    }

    private List<YieldEntity> getYieldEntities(List<YieldRequestDto> yields, String userId) {
        List<YieldEntity> yieldList = new ArrayList<>();
        for (YieldRequestDto yield : yields) {
            assetService.getAssetTypeByAssetName(yield.assetName());

            String yieldAt = generateYieldAt(yield.baseDate());
            String userAssetYieldAt = userId + yield.assetName() + yieldAt;

            if (!yieldRepository.existsByUserAssetYieldAt(userAssetYieldAt)) {
                yieldList.add(new YieldEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        yield.assetName(),
                        yieldAt,
                        userAssetYieldAt,
                        yield.baseDate(),
                        yield.paymentDate(),
                        yield.basePrice(),
                        yield.incomeValue(),
                        yield.yieldValue()));
            }
        }
        return yieldList;
    }

    private static List<YieldRequestDto> readCSVFile(MultipartFile file) {

        try {
            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser parser = new CSVParserBuilder().withSeparator(',').build();
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

    private static List<YieldRequestDto> processRows(List<String[]> rows) {

        List<YieldRequestDto> result = new ArrayList<>();
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

                result.add(new YieldRequestDto(
                        assetName,
                        baseLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        paymentLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        basePrice,
                        incomeValue,
                        yieldValue));
            } catch (DateTimeParseException e) {
                throw new InvalidDateFormatException(
                        "Erro na linha " + (rowNum + 2) + ": " + e.getMessage());
            } catch (NumberFormatException e) {
                throw new InvalidNumberFormatException(
                        "Erro na linha " + (rowNum + 2) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private static List<String> getYieldsAt(YieldTimeIntervalRequestDto payload) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        LocalDate startLocalDate = payload.startAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();
        LocalDate endLocalDate = payload.endAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();

        List<String> yieldAtParams = new ArrayList<>();

        YearMonth start = YearMonth.of(startLocalDate.getYear(), startLocalDate.getMonth());
        YearMonth end = YearMonth.of(endLocalDate.getYear(), endLocalDate.getMonth());

        while (!start.isAfter(end)) {
            yieldAtParams.add(start.format(formatter));
            start = start.plusMonths(1);
        }

        return yieldAtParams;
    }

    private static String generateYieldAt(Instant basePrice) {

        DateTimeFormatter formatted = DateTimeFormatter.ofPattern("yyyyMM");
        return basePrice.atZone(ZoneId.systemDefault()).format(formatted);
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
                    "Formato de cabeçalho inválido. Esperado: " + String.join(", ", expectedHeaders));
        }

        for (int i = 0; i < expectedHeaders.length; i++) {
            if (!header[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                throw new InvalidFileFormatException(
                        "Coluna inválida no cabeçalho. Esperado: '" + expectedHeaders[i] +
                                "', Encontrado: '" + header[i].trim() + "'");
            }
        }
    }

    private static void validateRowData(String[] row, int rowNum) {

        if (row.length != 7)
            throw new InvalidFileFormatException(
                    "A linha " + rowNum + " possui número incorreto de colunas");

        for (int i = 0; i < row.length; i++) {
            if (row[i] == null || row[i].trim().isEmpty()) {
                throw new InvalidFileFormatException(
                        "Na linha " + rowNum + ", a coluna " + (i + 1) + " está vazia");
            }
        }
    }

    private static void validateDate(LocalDate baseDate, LocalDate paymentDate) {

        if (paymentDate.isBefore(baseDate))
            throw new InvalidDateFormatException(
                    "A data de pagamento precisa ser maior que a data base de cálculo do dividendo");

        int currentYear = LocalDate.now().getYear();

        if (baseDate.getYear() > currentYear || paymentDate.getYear() > currentYear)
            throw new InvalidDateFormatException(
                    "O ano da data base e/ou da data de pagamento precisa ser menor ou igual a ano corrente");
    }

    private static void validateYieldAt(String yieldAt) {

        if (yieldAt.length() != 6)
            throw new InvalidStringFormatException(
                    "O yieldAt deve conter apenas 6 caracteres contendo o ano (yyyy) e o mês (mm)");

        int currentYear = LocalDate.now().getYear();

        for (int i = 0; i < yieldAt.length(); i++) {
            int year = Integer.parseInt(yieldAt.substring(0, 4));
            int month = Integer.parseInt(yieldAt.substring(4));

            if (year > currentYear)
                throw new InvalidStringFormatException(
                        "O ano informado no YieldAt deve ter 4 caracteres e ser menor ou igual ao ano corrente");

            if (month > 12)
                throw new InvalidStringFormatException(
                        "O mês informado no YieldAt deve ter 2 caracteres e ser válido");
        }
    }

    private static LocalDate parseDate(String dateStr, DateTimeFormatter formatter,
            String fieldName, int rowNum) {

        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(
                    "Erro na linha " + rowNum + ", " + fieldName +
                            ": formato de data inválido. Use dd/MM/yyyy");
        }
    }

    private static BigDecimal parseBigDecimal(String value, String fieldName, int rowNum) {

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidNumberFormatException(
                    "Erro na linha " + rowNum + ", " + fieldName +
                            ": valor numérico inválido");
        }
    }
}
