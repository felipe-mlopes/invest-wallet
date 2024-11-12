package personal.investwallet.modules.wallet;

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

        walletRepository.addPurchaseToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newPurchase, payload.purchaseAmount());

        return "A compra do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso" ;
    }

    @SneakyThrows
    public String addAllPurchasesToAssetByFile(String token, MultipartFile file) {

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
                    Instant purchaseDate = infoDto.date();
                    boolean exists = purchasesInfoList.stream()
                            .anyMatch(purchase -> purchase.getPurchaseDate().equals(purchaseDate));

                    if (!exists) {
                        PurchasesInfo purchasesInfo = new PurchasesInfo();
                        purchasesInfo.setPurchaseId(infoDto.id());
                        purchasesInfo.setPurchaseAmount(infoDto.amount());
                        purchasesInfo.setPurchasePrice(infoDto.price());
                        purchasesInfo.setPurchaseDate(infoDto.date());
                        purchasesInfo.setPurchaseQuotaValue(infoDto.quotaValue());

                        purchasesInfoList.add(purchasesInfo);
                        totalAmount += infoDto.amount();
                    }
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
            return "Uma carteira foi criada e os registros de vendas foram cadastrados com sucesso";
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

        walletRepository.addSaleToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newSale, saleAmount);

        return "A venda do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso" ;
    }

    @SneakyThrows
    public String addAllSalesToAssetByFile(String token, MultipartFile file) {

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
                    throw new ResourceNotFoundException("A quantidade de cotas do ativo não pode ser negativo");


                asset.setQuotaAmount(totalAmount);
                wallet.get().getAssets().put(assetName, asset);
            }

            walletRepository.save(wallet.get());
            return "Os registros de vendas foram cadastrados na carteira com sucesso";

        } else {
            WalletEntity newWallet = new WalletEntity();
            newWallet.setUserId(userId);

            for (Map.Entry<String, List<InfoGenericDto>> entry : saleList.entrySet()) {
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

                List<SalesInfo> salesInfoList = asset.getSalesInfo();
                int totalAmount = asset.getQuotaAmount();

                for (InfoGenericDto infoDto : infoDtoList) {
                    SalesInfo salesInfo = new SalesInfo();
                    salesInfo.setSaleId(infoDto.id());
                    salesInfo.setSaleAmount(infoDto.amount());
                    salesInfo.setSalePrice(infoDto.price());
                    salesInfo.setSaleDate(infoDto.date());
                    salesInfo.setSaleQuotaValue(infoDto.quotaValue());

                    salesInfoList.add(salesInfo);
                    totalAmount += infoDto.amount();
                }

                asset.setQuotaAmount(totalAmount);
                newWallet.getAssets().put(assetName, asset);
            }

            walletRepository.save(newWallet);
            return "Uma carteira foi criada e os registros de vendas foram cadastrados com sucesso";
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

    private static Map<String, List<InfoGenericDto>> readCSVFile(MultipartFile file) throws IOException, CsvException {
        Reader reader = new InputStreamReader(file.getInputStream());

        CSVParser parser = new CSVParserBuilder().withSeparator(',').build();
        CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

        List<String[]> rows = csvReader.readAll();
        Map<String, List<InfoGenericDto>> groupedInfoByAssetName = new HashMap<>();

        rows.remove(0);

        rows.forEach(row -> {
            for (int i = 0; i < row.length; i++)
                row[i] = row[i]
                        .replace(",", ".");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            String assetName = row[0];
            String date = row[1];
            String amount = row[2];
            String price = row[3];
            String quotaValue = row[4];

            LocalDate localDate = LocalDate.parse(date, formatter);
            Instant dateInstant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            int amountInt = Integer.parseInt(amount);
            BigDecimal priceDecimal = new BigDecimal(price.trim());
            BigDecimal quotaValueDecimal = new BigDecimal(quotaValue.trim());

            InfoGenericDto infoDto = new InfoGenericDto(
                    UUID.randomUUID().toString(),
                    amountInt,
                    priceDecimal,
                    quotaValueDecimal,
                    dateInstant
            );

            groupedInfoByAssetName.computeIfAbsent(assetName, k -> new ArrayList<>()).add(infoDto);
        });

        csvReader.close();

        return groupedInfoByAssetName;
    }

}

