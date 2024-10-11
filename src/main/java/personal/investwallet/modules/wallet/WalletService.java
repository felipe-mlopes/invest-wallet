package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import personal.investwallet.exceptions.*;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.AssetCreateRequestDto;
import personal.investwallet.modules.wallet.dto.PurchasesInfoRequestDto;
import personal.investwallet.modules.wallet.dto.SalesInfoRequestDto;
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

    public String addAssetToWallet(String token, AssetCreateRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        Asset newAsset = new Asset(
                payload.assetName(),
                payload.quotaAmount(),
                new HashSet<>(),
                new HashSet<>()
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

    public String addPurchaseToAsset(String token, PurchasesInfoRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para o usuário informado."));

        Asset asset = wallet.getAsset().get(payload.assetName());

        if (asset == null)
            throw new ForbiddenException("O ativo informado não existe na carteira.");

        PurchasesInfo newPurchase = new PurchasesInfo(
                payload.purchaseAmount(),
                payload.purchasePrice(),
                payload.purchaseDate()
        );

        walletRepository.addPurchaseToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newPurchase, payload.purchaseAmount());

        return "A compra do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso." ;
    }

    public String addSaleToAsset(String token, SalesInfoRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new ResourceNotFoundException("O ativo informado não existe.");

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada para o usuário informado."));

        Asset asset = wallet.getAsset().get(payload.assetName());

        if (asset == null)
            throw new ForbiddenException("O ativo informado não existe na carteira.");

        int saleAmount = payload.saleAmount() * -1;

        if (asset.getQuotaAmount() + saleAmount < 0)
            throw new BadRequestException("A quantidade de cota do ativo não pode ser negativa.");

        SalesInfo newSale = new SalesInfo(
                saleAmount,
                payload.salePrice(),
                payload.saleDate()
        );

        walletRepository.addSaleToAssetByUserIdAndAssetName(userId, asset.getAssetName(), newSale, payload.saleAmount());

        return "A venda do seu ativo " + asset.getAssetName() + " foi cadastrada com sucesso." ;
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

}

