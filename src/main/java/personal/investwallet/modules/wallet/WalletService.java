package personal.investwallet.modules.wallet;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.SneakyThrows;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.exceptions.*;
import personal.investwallet.modules.asset.AssetService;
import personal.investwallet.modules.wallet.dto.*;
import personal.investwallet.security.TokenService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static personal.investwallet.modules.wallet.WalletEntity.*;
import static personal.investwallet.modules.wallet.WalletEntity.Asset.*;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AssetService assetService;

    public List<String> getAllAssetNames() {

        return walletRepository.findDistinctAssetNames();
    }

    public List<String> getAllUserIdsWithWalletCreatedByAssetName(String assetName) {

        return walletRepository.findUserIdsByAssetKey(assetName);
    }

    public Integer getQuotaAmountOfAssetByUserId(String userId, String assetName) {

        Optional<Document> result = walletRepository.findQuotaAmountByUserIdAndAssetKey(userId, assetName);

        return result.map(doc -> {
            Document assets = (Document) doc.get("assets");

            if (assets != null) {
                Document asset = (Document) assets.get(assetName);

                if (asset != null)
                    return asset.getInteger("quota_amount");
            }

            return null;
        }).orElse(null);
    }

    public String addAssetToWallet(String token, CreateAssetRequestDto payload) {

        String userId = tokenService.extractUserIdFromToken(token);
        verifyAssetNameExists(payload.assetName());

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        Asset newAsset = new Asset(
                payload.assetName(),
                0,
                new ArrayList<>(),
                new ArrayList<>()
        );

        if (wallet.isPresent()) {

            if (wallet.get().getAssets().containsKey(payload.assetName()))
                throw new ConflictException("O ativo informado já existe na carteira");

            walletRepository.addNewAssetByUserId(userId, newAsset.getAssetName(), newAsset);

            return "O ativo " + payload.assetName() + " foi adicionado à carteira com sucesso";

        } else {

            WalletEntity newWallet = new WalletEntity();
            newWallet.setUserId(userId);
            newWallet.getAssets().put(newAsset.getAssetName(), newAsset);

            walletRepository.save(newWallet);

            return "Uma nova carteira foi criada e o ativo " + payload.assetName() + " foi adicionado";
        }
    }

    public String addPurchaseToAsset(String token, AddPurchaseRequestDto payload) {

        String userId = getUserId(token);

        WalletEntity.Asset asset = getAssetVerified(payload.assetName(),userId);

        BigDecimal purchaseQuotaValue = payload.purchasePrice().divideToIntegralValue(new BigDecimal(payload.purchaseAmount()));

        PurchasesInfo newPurchase = new PurchasesInfo(
                UUID.randomUUID().toString(),
                payload.purchaseAmount(),
                payload.purchasePrice(),
                purchaseQuotaValue,
                payload.purchaseDate()
        );

        walletRepository.addPurchaseToAsset(userId, asset.getAssetName(), newPurchase, payload.purchaseAmount());

        return "A compra do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso" ;
    }

    @SneakyThrows
    public String addManyPurchasesToAssetByFile(String token, MultipartFile file) {

        String userId = tokenService.extractUserIdFromToken(token);

        Map<String, List<InfoGenericDto>> purchaseList = readCSVFile(file);

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        if (wallet.isPresent()) {
            for (Map.Entry<String, List<InfoGenericDto>> entry : purchaseList.entrySet()) {
                String assetName = entry.getKey();
                List<InfoGenericDto> infoDtoList = entry.getValue();

                // Verifica se o nome do ativo existe
                verifyAssetNameExists(assetName);

                Asset asset = wallet.get().getAssets().getOrDefault(assetName, new Asset());
                asset.setAssetName(assetName);

                List<PurchasesInfo> purchasesInfoList = asset.getPurchasesInfo();

                if (purchasesInfoList == null) {
                    purchasesInfoList = new ArrayList<>();
                    asset.setPurchasesInfo(purchasesInfoList);
                }

                int totalAmount = asset.getQuotaAmount();

                for (InfoGenericDto infoDto : infoDtoList) {
                    PurchasesInfo purchasesInfo = new PurchasesInfo();
                    purchasesInfo.setPurchaseId(infoDto.id());
                    purchasesInfo.setPurchaseAmount(infoDto.amount());
                    purchasesInfo.setPurchasePrice(infoDto.price());
                    purchasesInfo.setPurchaseDate(infoDto.date());
                    purchasesInfo.setPurchaseQuotaValue(infoDto.quotaValue());

                    purchasesInfoList.add(purchasesInfo);
                    totalAmount += infoDto.amount();
                }

                asset.setQuotaAmount(totalAmount);
                wallet.get().getAssets().put(assetName, asset);
            }

            walletRepository.save(wallet.get());
            return "Os registros de compras foram cadastrados na carteira com sucesso";

        } else {
            WalletEntity newWallet = new WalletEntity();
            newWallet.setUserId(userId);

            for (Map.Entry<String, List<InfoGenericDto>> entry : purchaseList.entrySet()) {
                String assetName = entry.getKey();
                List<InfoGenericDto> infoDtoList = entry.getValue();

                // Verifica se o nome do ativo existe
                verifyAssetNameExists(assetName);

                Asset asset = new Asset(
                        assetName,
                        0,
                        new ArrayList<>(),
                        new ArrayList<>()
                );

                List<PurchasesInfo> purchasesInfoList = asset.getPurchasesInfo();
                int totalAmount = asset.getQuotaAmount();

                for (InfoGenericDto infoDto : infoDtoList) {
                    PurchasesInfo purchasesInfo = new PurchasesInfo();
                    purchasesInfo.setPurchaseId(infoDto.id());
                    purchasesInfo.setPurchaseAmount(infoDto.amount());
                    purchasesInfo.setPurchasePrice(infoDto.price());
                    purchasesInfo.setPurchaseDate(infoDto.date());
                    purchasesInfo.setPurchaseQuotaValue(infoDto.quotaValue());

                    purchasesInfoList.add(purchasesInfo);
                    totalAmount += infoDto.amount();
                }

                asset.setQuotaAmount(totalAmount);
                newWallet.getAssets().put(assetName, asset);
            }

            walletRepository.save(newWallet);
            return "Uma carteira foi criada e os registros de compras foram cadastrados com sucesso";
        }

    }

    public String updatePurchaseToAssetByPurchaseId(
            String token,
            String assetName,
            String purchaseId,
            UpdatePurchaseRequestDto payload
    ) {

        if (payload.purchaseAmount() == null && payload.purchasePrice() == null && payload.purchaseDate() == null)
            throw new BadRequestException("Não há informações de compra para serem atualizadas");

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        Optional<PurchasesInfo> purchaseSelected = asset.getPurchasesInfo().stream()
                .filter(purchase -> purchase.getPurchaseId().equals(purchaseId))
                .findFirst();

        if (purchaseSelected.isEmpty()) {
            throw new ResourceNotFoundException("Não existe compra com o ID informado");
        }

        int purchaseAmount = payload.purchaseAmount() != null ? payload.purchaseAmount() : purchaseSelected.get().getPurchaseAmount();
        BigDecimal purchasePrice = payload.purchasePrice() != null ? payload.purchasePrice() : purchaseSelected.get().getPurchasePrice();
        Instant purchaseDate = payload.purchaseDate() != null ? payload.purchaseDate() : purchaseSelected.get().getPurchaseDate();

        if (payload.purchaseAmount() != null && purchaseSelected.get().getPurchaseAmount() != payload.purchaseAmount()) {
            int purchaseAmountRestored = purchaseSelected.get().getPurchaseAmount() * - 1;
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, purchaseAmountRestored);
        }

        BigDecimal purchaseQuotaValue = purchasePrice.divideToIntegralValue(new BigDecimal(purchaseAmount));

        asset.getPurchasesInfo().add(new PurchasesInfo(
                purchaseId,
                purchaseAmount,
                purchasePrice,
                purchaseQuotaValue,
                purchaseDate
        ));

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), purchaseAmount);

        return "A compra " + purchaseId + " do ativo " + assetName + " foi atualizada com sucesso";
    }

    public String removePurchaseToAssetByPurchaseId(String token, String assetName, String purchaseId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        int purchaseAmount = asset.getPurchasesInfo().stream()
                .filter(purchase -> purchase.getPurchaseId().equals(purchaseId))
                .findFirst()
                .map(purchase -> -1 * purchase.getPurchaseAmount())
                .orElseThrow(() -> new ResourceNotFoundException("Compra com o ID fornecido não encontrada"));

        asset.getPurchasesInfo().removeIf(purchase -> purchase.getPurchaseId().equals(purchaseId));

        asset.setQuotaAmount(asset.getQuotaAmount() + purchaseAmount);

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), purchaseAmount);

        return "A compra " + purchaseId + " do ativo " + assetName + " foi removida com sucesso";
    }

    public String addSaleToAsset(String token, AddSaleRequestDto payload) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(payload.assetName(), userId);

        int saleAmount = payload.saleAmount() * -1;

        if (asset.getQuotaAmount() + saleAmount < 0)
            throw new BadRequestException("A quantidade de cota do ativo não pode ser negativa");

        BigDecimal saleQuotaValue = payload.salePrice().divideToIntegralValue(new BigDecimal(payload.saleAmount()));

        SalesInfo newSale = new SalesInfo(
                UUID.randomUUID().toString(),
                payload.saleAmount(),
                payload.salePrice(),
                saleQuotaValue,
                payload.saleDate()
        );

        walletRepository.addSaleToAsset(userId, asset.getAssetName(), newSale, saleAmount);

        return "A venda do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso" ;
    }

    @SneakyThrows
    public String addManySalesToAssetByFile(String token, MultipartFile file) {

        String userId = tokenService.extractUserIdFromToken(token);

        Map<String, List<InfoGenericDto>> saleList = readCSVFile(file);

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        if (wallet.isPresent()) {
            for (Map.Entry<String, List<InfoGenericDto>> entry : saleList.entrySet()) {
                String assetName = entry.getKey();
                List<InfoGenericDto> infoDtoList = entry.getValue();

                // Verifica se o nome do ativo existe
                verifyAssetNameExists(assetName);

                Asset asset = wallet.get().getAssets().getOrDefault(assetName, new Asset());
                asset.setAssetName(assetName);

                List<SalesInfo> salesInfoList = asset.getSalesInfo();

                if (salesInfoList == null) {
                    salesInfoList = new ArrayList<>();
                    asset.setSalesInfo(salesInfoList);
                }

                int totalAmount = asset.getQuotaAmount();

                for (InfoGenericDto infoDto : infoDtoList) {
                    Instant saleDate = infoDto.date();
                    boolean exists = salesInfoList.stream()
                            .anyMatch(purchase -> purchase.getSaleDate().equals(saleDate));

                    if (!exists) {
                        SalesInfo salesInfo = new SalesInfo();
                        salesInfo.setSaleId(infoDto.id());
                        salesInfo.setSaleAmount(infoDto.amount());
                        salesInfo.setSalePrice(infoDto.price());
                        salesInfo.setSaleDate(infoDto.date());
                        salesInfo.setSaleQuotaValue(infoDto.quotaValue());

                        salesInfoList.add(salesInfo);
                        totalAmount -= infoDto.amount();
                    }
                }

                if (totalAmount < 0)
                    throw new BadRequestException("A quantidade de cotas do ativo não pode ser negativa");


                asset.setQuotaAmount(totalAmount);
                wallet.get().getAssets().put(assetName, asset);
            }

            walletRepository.save(wallet.get());
            return "Os registros de vendas foram cadastrados na carteira com sucesso";

        } else {
            throw new BadRequestException("Não é possível adicionar um venda a uma nova carteira antes de inserir uma compra");
        }
    }

    public String updateSaleToAssetBySaleId(
            String token,
            String assetName,
            String saleId,
            UpdateSaleRequestDto payload
    ) {

        if (payload.saleAmount() == null && payload.salePrice() == null && payload.saleDate() == null)
            throw new BadRequestException("Não há informações de venda para serem atualizadas");

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        Optional<SalesInfo> saleSelected = asset.getSalesInfo().stream()
                .filter(sale -> sale.getSaleId().equals(saleId))
                .findFirst();

        if (saleSelected.isEmpty()) {
            throw new ResourceNotFoundException("Não existe venda com o ID informado");
        }

        int saleAmount = payload.saleAmount() != null ? payload.saleAmount() : saleSelected.get().getSaleAmount();
        BigDecimal salePrice = payload.salePrice() != null ? payload.salePrice() : saleSelected.get().getSalePrice();
        Instant saleDate = payload.saleDate() != null ? payload.saleDate() : saleSelected.get().getSaleDate();
        BigDecimal saleQuotaValue = salePrice.divideToIntegralValue(new BigDecimal(saleAmount));

        if (payload.saleAmount() != null && saleSelected.get().getSaleAmount() != payload.saleAmount()) {
            int saleAmountRestored = saleSelected.get().getSaleAmount();
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, saleAmountRestored);
        }

        asset.getSalesInfo().add(new SalesInfo(
                saleId,
                saleAmount,
                salePrice,
                saleQuotaValue,
                saleDate
        ));

        walletRepository.updateSaleInAssetBySaleId(userId, assetName, asset.getSalesInfo(), saleAmount);

        return "A venda " + saleId + " do ativo " + assetName + " foi atualizada com sucesso";
    }

    public String removeSaleToAssetBySaleId(String token, String assetName, String saleId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName, userId);

        int saleAmount = asset.getSalesInfo().stream()
                .filter(sale -> sale.getSaleId().equals(saleId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Venda com o ID fornecido não encontrada"))
                .getSaleAmount();

        asset.getSalesInfo().removeIf(sale -> sale.getSaleId().equals(saleId));

        asset.setQuotaAmount(asset.getQuotaAmount() + saleAmount);

        walletRepository.updateSaleInAssetBySaleId(userId, assetName, asset.getSalesInfo(), saleAmount);

        return "A venda " + saleId + " do ativo " + assetName + " foi removida com sucesso";
    }

    private String getUserId(String token) {
        return tokenService.extractUserIdFromToken(token);
    }

    private void verifyAssetNameExists(String assetName) {
        assetService.getAssetTypeByAssetName(assetName);
    }

    private WalletEntity.Asset getAssetVerified(String assetName, String userId) {

        verifyAssetNameExists(assetName);

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para o usuário informado"));

        Asset asset = wallet.getAssets().get(assetName);

        if (asset == null)
            throw new BadRequestException("O ativo informado não existe na carteira");

        return asset;
    }

    private static Map<String, List<InfoGenericDto>> readCSVFile(MultipartFile file) {

        try {
            // Verifica se o arquivo está vazio ou em um formato inválido
            validateFile(file);

            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser parser = new CSVParserBuilder().withSeparator(',').build();
            CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

            List<String[]> rows = csvReader.readAll();

            // Verifica se cabeçalho segue o padrão desejado
            validateHeader(rows.get(0));

            // Remove o cabeçalho
            rows.remove(0);

            Map<String, List<InfoGenericDto>> groupedInfoByAssetName = processRows(rows);
            csvReader.close();

            return groupedInfoByAssetName;

        } catch (IOException e) {
            throw new FileProcessingException("Erro ao ler o arquivo CSV");
        } catch (CsvException e) {
            throw new InvalidFileFormatException("Erro ao processar o arquivo CSV");
        }
    }

    private static Map<String, List<InfoGenericDto>> processRows(List<String[]> rows) {

        Map<String, List<InfoGenericDto>> groupedInfoByAssetName = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            String[] row = rows.get(rowNum);
            for (int i = 0; i < row.length; i++)
                row[i] = row[i]
                        .replace(",", ".");

            // Verifica a quantidade colunas de cada linha
            validateRowData(row, rowNum);

            String assetName = row[0].trim();

            LocalDate date = parseDate(row[1].trim(), formatter, rowNum);
            Instant dateInstant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();

            // Verifica se data informada é menor ou igual a data corrente
            validateDate(date);

            int amountInt = parseInteger(row[2].trim(), rowNum);

            BigDecimal priceDecimal = parseBigDecimal(row[3].trim(), "Preço", rowNum);
            BigDecimal quotaValueDecimal = parseBigDecimal(row[4].trim(), "Valor da cota", rowNum);

            InfoGenericDto infoDto = new InfoGenericDto(
                    UUID.randomUUID().toString(),
                    amountInt,
                    priceDecimal,
                    quotaValueDecimal,
                    dateInstant
            );

            groupedInfoByAssetName.computeIfAbsent(assetName, k -> new ArrayList<>()).add(infoDto);
        }

        return groupedInfoByAssetName;
    }

    private static void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("O arquivo não enviado ou não preenchido");
        }

        String filename = file.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileFormatException("O arquivo deve ser um CSV válido");
        }
    }

    private static void validateHeader(String[] header) {

        String[] expectedHeaders = {
                "Asset Name", "Date", "Amount", "Quota Price", "Value / Quota"
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

        if (row.length != 5)
            throw new InvalidFileFormatException(
                    "A linha " + (rowNum + 2) + " possui número incorreto de colunas"
            );


        for (int i = 0; i < row.length; i++) {
            if (row[i].trim().isEmpty()) {
                throw new InvalidFileFormatException(
                        "Na linha " + (rowNum + 2) + ", a coluna " + (i + 1) + " está vazia"
                );
            }
        }
    }

    private static void validateDate(LocalDate date) {

        int currentYear = LocalDate.now().getYear();

        if (date.getYear() > currentYear)
            throw new InvalidDateFormatException(
                    "A data informada precisa ser menor ou igual a data corrente"
            );
    }

    private static LocalDate parseDate(String dateStr, DateTimeFormatter formatter,
                                       int rowNum) {

        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(
                    "Erro na linha " + (rowNum + 2) + ", " + "Data" +
                            ": formato de data inválido. Use dd/MM/yyyy"
            );
        }
    }

    private static Integer parseInteger(String value, int rowNum) {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidNumberFormatException(
                    "Erro na linha " + (rowNum + 2) + ", Quantidade: valor numérico inválido"
            );
        }
    }

    private static BigDecimal parseBigDecimal(String value, String fieldName, int rowNum) {

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidNumberFormatException(
                    "Erro na linha " + (rowNum + 2) + ", " + fieldName +
                            ": valor numérico inválido"
            );
        }
    }
}

