package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import personal.investwallet.exceptions.*;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.*;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.security.TokenService;

import java.util.*;

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
    private ScraperService scraperService;

    public String addAssetToWallet(String token, CreateAssetRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        Asset newAsset = new Asset(
                payload.assetName(),
                payload.quotaAmount(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        if (wallet.isPresent()) {

            if (wallet.get().getAsset().containsKey(payload.assetName()))
                throw new ConflictException("O ativo informado já existe na carteira.");

            walletRepository.addNewAssetByUserId(userId, newAsset.getAssetName(), newAsset);

        } else {

            WalletEntity newWallet = new WalletEntity();
            newWallet.setUserId(userId);
            newWallet.getAsset().put(newAsset.getAssetName(), newAsset);

            walletRepository.save(newWallet);
        }

        return "Ativo adicionado à carteira com sucesso!";
    }

    public String addPurchaseToAsset(String token, AddPurchaseRequestDto payload) {

        String userId = getUserId(token);

        WalletEntity.Asset asset = getAssetVerified(payload.assetType(), payload.assetName(),userId);

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
            String assetType,
            String assetName,
            String purchaseId,
            UpdatePurchaseRequestDto payload
    ) {

        String userId = getUserId(token);

        WalletEntity.Asset asset = getAssetVerified(assetType, assetName,userId);

        PurchasesInfo purchaseSelected = asset.getPurchasesInfo().stream()
                .filter(purchase -> purchase.getPurchaseId().equals(purchaseId))
                .toList()
                .get(0);

        int purchaseAmount = 0;

        if (payload.purchaseAmount() != null && purchaseSelected.getPurchaseAmount() != payload.purchaseAmount()) {
            int purchaseAmountRestored = purchaseSelected.getPurchaseAmount() * - 1;
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, purchaseAmountRestored);
            purchaseAmount = payload.purchaseAmount();
        }

        boolean purchaseInfoRemove = asset.getPurchasesInfo().removeIf(purchase -> purchase.getPurchaseId().equals(purchaseId));

        if (!purchaseInfoRemove) {
            throw new ResourceNotFoundException("Compra com o ID fornecido não encontrada.");
        }

        asset.getPurchasesInfo().add(new PurchasesInfo(
                purchaseId,
                payload.purchaseAmount(),
                payload.purchasePrice(),
                payload.purchaseDate()
        ));

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), purchaseAmount);

        return "A compra " + purchaseId + " do ativo " + assetName + " foi atualizada com sucesso.";
    }

    public String removePurchaseToAssetByPurchaseId(String token, String assetType, String assetName, String purchaseId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetType, assetName,userId);

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

        Asset asset = getAssetVerified(payload.assetType(), payload.assetName(), userId);

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
            String assetType,
            String assetName,
            String saleId,
            UpdateSaleRequestDto payload
    ) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetType, assetName,userId);

        SalesInfo saleSelected = asset.getSalesInfo().stream()
                .filter(sale -> sale.getSaleId().equals(saleId))
                .toList()
                .get(0);

        int saleAmount = 0;

        if (payload.saleAmount() != null && saleSelected.getSaleAmount() != payload.saleAmount()) {
            int saleAmountRestored = saleSelected.getSaleAmount() * - 1;
            walletRepository.restoreAmountOfQuotasInAsset(userId, assetName, saleAmountRestored);
            saleAmount = payload.saleAmount();
        }

        boolean purchaseInfoRemove = asset.getPurchasesInfo().removeIf(purchase -> purchase.getPurchaseId().equals(saleId));

        if (!purchaseInfoRemove) {
            throw new ResourceNotFoundException("Compra com o ID fornecido não encontrada.");
        }

        asset.getPurchasesInfo().add(new PurchasesInfo(
                saleId,
                payload.saleAmount(),
                payload.salePrice(),
                payload.saleDate()
        ));

        walletRepository.updatePurchaseInAssetByPurchaseId(userId, assetName, asset.getPurchasesInfo(), saleAmount);

        return "A venda " + saleId + " do ativo " + assetName + " foi atualizada com sucesso.";
    }

    public String removeSaleToAssetBySaleId(String token, String assetType, String assetName, String saleId) {

        String userId = getUserId(token);

        Asset asset = getAssetVerified(assetType, assetName, userId);

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

        userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Não é permitido inserir um ativo na carteira sem estar logado."));
        return userId;
    }

    private boolean verifyAssetNameExists(String assetType, String assetName) {
        return scraperService.verifyIfWebsiteIsValid(assetType, assetName);
    }

    private WalletEntity.Asset getAssetVerified(String assetType, String assetName, String userId) {

        boolean isAssetExists = verifyAssetNameExists(assetType, assetName);

        if (!isAssetExists)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para o usuário informado."));

        Asset asset = wallet.getAsset().get(assetName);

        if (asset == null)
            throw new ForbiddenException("O ativo informado não existe na carteira.");

        return asset;
    }

}

