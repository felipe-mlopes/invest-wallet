package personal.investwallet.modules.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.AssetCreateRequestDto;
import personal.investwallet.modules.wallet.dto.PurchasesInfoRequestDto;
import personal.investwallet.modules.wallet.dto.SalesInfoRequestDto;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.security.TokenService;

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
    private ScraperService scraperService;

    private static PurchasesInfo purchases(PurchasesInfoRequestDto payload) {
        return new PurchasesInfo(
                payload.purchaseAmount(),
                payload.purchasePrice(),
                payload.purchaseDate()
        );
    }

    private static SalesInfo sales(SalesInfoRequestDto payload) {
        return new SalesInfo(
                payload.saleAmount(),
                payload.salePrice(),
                payload.saleDate()
        );
    }

    public String addAssetToWallet(String token, AssetCreateRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new RuntimeException("O ativo informado não existe.");

        Optional<WalletEntity> wallet = walletRepository.findByUserId(userId);

        if (wallet.isPresent()) {

            // Implementar a lógica de adicionar um novo asset na carteira existente

        } else {

            // Criar uma carteira para userId adicionando o asset informado

        }

        return "Ativo adicionado à carteira com sucesso!";
    }

    public String registerPurchaseOfAnExistingAsset(String token, PurchasesInfoRequestDto payload) {

        String userId = getUserId(token);

        boolean isAssetExists = verifyAssetNameExists(payload.assetType(), payload.assetName());

        if (!isAssetExists)
            throw new RuntimeException("O ativo informado não existe.");

        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Carteira não encontrada para o usuário informado."));

        Asset asset = wallet.getAsset().get(payload.assetName());

        if (asset == null)
            throw new RuntimeException("O ativo informado não existe na carteira");

        PurchasesInfo newPurchase = new PurchasesInfo(
                payload.purchaseAmount(),
                payload.purchasePrice(),
                payload.purchaseDate()
        );

        walletRepository.updatePurchasesInfo(userId, asset.getAssetName(), newPurchase, payload.purchaseAmount());

        return "A compra do seu ativo " + payload.assetName() + " foi cadastrada com sucesso" ;
    }

    private String getUserId(String token) {
        String userId = tokenService.extractUserIdFromToken(token);

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Não é permitido inserir um ativo na carteira sem estar logado"));
        return userId;
    }

    private boolean verifyAssetNameExists(String assetType, String assetName) {
        return scraperService.verifyIfWebsiteIsValid(assetType, assetName);
    }

    private Asset convertToAssetEntity(AssetCreateRequestDto asset) {
        Set<PurchasesInfo> purchasesInfo = (asset.purchasesInfo() == null) ? new HashSet<>() : asset.purchasesInfo().stream()
                .map(WalletService::purchases).collect(Collectors.toSet());

        Set<SalesInfo> salesInfo = (asset.salesInfo() == null) ? new HashSet<>() : asset.salesInfo().stream()
                .map(WalletService::sales).collect(Collectors.toSet());

        return new WalletEntity.Asset(
                asset.assetName(),
                asset.quotaAmount(),
                purchasesInfo,
                salesInfo
        );

    }
}

