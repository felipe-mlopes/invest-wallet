package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import personal.investwallet.exceptions.*;
import personal.investwallet.modules.asset.AssetService;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.*;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.security.TokenService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static personal.investwallet.modules.wallet.WalletEntity.*;
import static personal.investwallet.modules.wallet.WalletEntity.Asset.*;

@Service
public class WalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AssetService assetService;

    public List<Object> getAllAssets(String token) {

        String userId = tokenService.extractUserIdFromToken(token);

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma carteira foi localizada para esse usuário."));;

        return wallet.getAssets().values().stream()
                .map(asset -> Map.of(
                        "assetName", asset.getAssetName(),
                        "assetQuotaAmount", asset.getQuotaAmount()
                ))
                .collect(Collectors.toList());
    }

    public GetQuotaAmountResponseDto getQuotaAmountOfAnAsset(String token, String assetName) {

        String userId = tokenService.extractUserIdFromToken(token);

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma carteira foi localizada para esse usuário."));;

        Asset asset = wallet.getAssets().get(assetName);

        return new GetQuotaAmountResponseDto(asset.getQuotaAmount());
    }

    public String addAssetToWallet(String token, CreateAssetRequestDto payload) {

        String userId = tokenService.extractUserIdFromToken(token);
        String assetType = verifyAssetNameExists(payload.assetName());

        if (assetType == null)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        Asset newAsset = new Asset(
                payload.assetName(),
                0,
                new ArrayList<>(),
                new ArrayList<>()
        );

        if (wallet.isPresent()) {

            if (wallet.get().getAssets().containsKey(payload.assetName()))
                throw new ConflictException("O ativo informado já existe na carteira.");

            walletRepository.addNewAssetByUserId(userId, newAsset.getAssetName(), newAsset);

            return "O ativo " + payload.assetName() + " foi adicionado à carteira com sucesso.";

        } else {

            WalletEntity newWallet = new WalletEntity();
            newWallet.setUserId(userId);
            newWallet.getAssets().put(newAsset.getAssetName(), newAsset);

            walletRepository.save(newWallet);

            return "Uma nova carteira foi criada e o ativo " + payload.assetName() + " foi adicionado.";
        }
    }

    public String addPurchaseToAsset(String token, AddPurchaseRequestDto payload) {

        String userId = getUserId(token);

        WalletEntity.Asset asset = getAssetVerified(payload.assetName(),userId);

        PurchasesInfo newPurchase = new PurchasesInfo(
                UUID.randomUUID().toString(),
                payload.purchaseAmount(),
                payload.purchasePrice(),
                payload.purchaseDate()
        );

        walletRepository.addPurchaseToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newPurchase, payload.purchaseAmount());

        return "A compra do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso." ;
    }

    public String updatePurchaseToAssetByPurchaseId(
            String token,
            String assetName,
            String purchaseId,
            UpdatePurchaseRequestDto payload
    ) {

        if (payload.purchaseAmount() == null && payload.purchasePrice() == null && payload.purchaseDate() == null)
            throw new BadRequestException("Não há informações de compra para serem atualizadas.");

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        Optional<PurchasesInfo> purchaseSelected = asset.getPurchasesInfo().stream()
                .filter(purchase -> purchase.getPurchaseId().equals(purchaseId))
                .findFirst();

        if (purchaseSelected.isEmpty()) {
            throw new ResourceNotFoundException("Não existe compra com o ID informado.");
        }

        int purchaseAmount = payload.purchaseAmount() != null ? payload.purchaseAmount() : purchaseSelected.get().getPurchaseAmount();
        BigDecimal purchasePrice = payload.purchasePrice() != null ? payload.purchasePrice() : purchaseSelected.get().getPurchasePrice();
        Instant purchaseDate = payload.purchaseDate() != null ? payload.purchaseDate() : purchaseSelected.get().getPurchaseDate();

        if (payload.purchaseAmount() != null && purchaseSelected.get().getPurchaseAmount() != payload.purchaseAmount()) {
            int purchaseAmountRestored = purchaseSelected.get().getPurchaseAmount() * - 1;
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, purchaseAmountRestored);
        }

        asset.getPurchasesInfo().add(new PurchasesInfo(
                purchaseId,
                purchaseAmount,
                purchasePrice,
                purchaseDate
        ));

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), purchaseAmount);

        return "A compra " + purchaseId + " do ativo " + assetName + " foi atualizada com sucesso.";
    }

    public String removePurchaseToAssetByPurchaseId(String token, String assetName, String purchaseId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        int purchaseAmount = - 1 * asset.getPurchasesInfo().stream()
                .filter(purchase -> purchase.getPurchaseId().equals(purchaseId))
                .toList()
                .get(0)
                .getPurchaseAmount();

        boolean purchaseInfoRemove = asset.getPurchasesInfo().removeIf(purchase -> purchase.getPurchaseId().equals(purchaseId));

        if (!purchaseInfoRemove) {
            throw new ResourceNotFoundException("Compra com o ID fornecido não encontrada.");
        }

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), purchaseAmount);

        return "A compra " + purchaseId + " do ativo " + assetName + " foi removida com sucesso.";
    }

    public String addSaleToAsset(String token, AddSaleRequestDto payload) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(payload.assetName(), userId);

        int saleAmount = payload.saleAmount() * -1;

        if (asset.getQuotaAmount() + saleAmount < 0)
            throw new BadRequestException("A quantidade de cota do ativo não pode ser negativa.");

        SalesInfo newSale = new SalesInfo(
                UUID.randomUUID().toString(),
                payload.saleAmount(),
                payload.salePrice(),
                payload.saleDate()
        );

        walletRepository.addSaleToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newSale, saleAmount);

        return "A venda do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso." ;
    }

    public String updateSaleToAssetBySaleId(
            String token,
            String assetName,
            String saleId,
            UpdateSaleRequestDto payload
    ) {

        if (payload.saleAmount() == null && payload.salePrice() == null && payload.saleDate() == null)
            throw new BadRequestException("Não há informações de venda para serem atualizadas.");

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName,userId);

        Optional<SalesInfo> saleSelected = asset.getSalesInfo().stream()
                .filter(sale -> sale.getSaleId().equals(saleId))
                .findFirst();

        if (saleSelected.isEmpty()) {
            throw new ResourceNotFoundException("Não existe venda com o ID informado.");
        }

        int saleAmount = payload.saleAmount() != null ? payload.saleAmount() : saleSelected.get().getSaleAmount();
        BigDecimal salePrice = payload.salePrice() != null ? payload.salePrice() : saleSelected.get().getSalePrice();
        Instant saleDate = payload.saleDate() != null ? payload.saleDate() : saleSelected.get().getSaleDate();

        if (payload.saleAmount() != null && saleSelected.get().getSaleAmount() != payload.saleAmount()) {
            int saleAmountRestored = saleSelected.get().getSaleAmount();
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, saleAmountRestored);
        }

        asset.getSalesInfo().add(new SalesInfo(
                saleId,
                saleAmount,
                salePrice,
                saleDate
        ));

        walletRepository.updateSaleInAssetBySaleId(userId, assetName, asset.getSalesInfo(), saleAmount);

        return "A venda " + saleId + " do ativo " + assetName + " foi atualizada com sucesso.";
    }

    public String removeSaleToAssetBySaleId(String token, String assetName, String saleId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetName, userId);

        int saleAmount = asset.getSalesInfo().stream()
                .filter(sale -> sale.getSaleId().equals(saleId))
                .toList()
                .get(0)
                .getSaleAmount();

        boolean saleInfoRemove = asset.getSalesInfo().removeIf(sale -> sale.getSaleId().equals(saleId));

        if (!saleInfoRemove) {
            throw new ResourceNotFoundException("Venda com o ID fornecido não encontrada.");
        }

        walletRepository.updateSaleInAssetBySaleId(userId, assetName, asset.getSalesInfo(), saleAmount);

        return "A venda " + saleId + " do ativo " + assetName + " foi removida com sucesso.";
    }

    private String getUserId(String token) {
        String userId = tokenService.extractUserIdFromToken(token);

        boolean isWalletExist = walletRepository.existsByUserId(userId);

        if (!isWalletExist)
            throw new ResourceNotFoundException("Nenhuma carteira foi localizada para esse usuário.");

        return userId;
    }

    private String verifyAssetNameExists(String assetName) {
        return assetService.getAssetTypeByAssetName(assetName);
    }

    private WalletEntity.Asset getAssetVerified(String assetName, String userId) {

        String assetType = verifyAssetNameExists(assetName);

        if (assetType == null)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para o usuário informado."));

        Asset asset = wallet.getAssets().get(assetName);

        if (asset == null)
            throw new ForbiddenException("O ativo informado não existe na carteira.");

        return asset;
    }

}

