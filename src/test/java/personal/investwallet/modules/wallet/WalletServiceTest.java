package personal.investwallet.modules.wallet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import personal.investwallet.exceptions.BadRequestException;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.ForbiddenException;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.AssetCreateRequestDto;
import personal.investwallet.modules.wallet.dto.PurchasesInfoRequestDto;
import personal.investwallet.modules.wallet.dto.SalesInfoRequestDto;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.security.TokenService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    public static final String TOKEN = "validToken";
    public static final String USER_ID = "user1234";
    public static final String ASSET_NAME = "ABCD11";
    public static final String ASSET_TYPE = "fundos-imobiliarios";

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private ScraperService scraperService;

    @InjectMocks
    private WalletService walletService;

    @Nested
    class addAssetToWallet {

        @Test
        @DisplayName("Should be able to create a new wallet by adding an asset")
        void shouldBeAbleToCreateNewWalletByAddingAnAsset() {

            // ARRANGE
            AssetCreateRequestDto payload = getAssetCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            // ACT
            String result = walletService.addAssetToWallet(TOKEN, payload);

            // ASSERT
            verify(walletRepository, times(1)).save(any(WalletEntity.class));
            assertEquals("Ativo adicionado à carteira com sucesso!", result);

        }

        @Test
        @DisplayName("Should be able to add a new asset to wallet")
        void shouldBeAbleToAddNewAssetToWallet() {

            // ARRANGE
            AssetCreateRequestDto payload = getAssetCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            // ACT
            String result = walletService.addAssetToWallet(TOKEN, payload);

            // ASSERT
            verify(walletRepository,times(1)).addNewAssetByUserId(eq(USER_ID), eq(payload.assetName()),any(WalletEntity.Asset.class));
            assertEquals("Ativo adicionado à carteira com sucesso!", result);
        }

        @Test
        @DisplayName("Should not be able to create a new wallet or add a new asset with an asset that does not exist")
        void shouldNotBeAbleToCreateNewWalletOrAddNewAssetWithAnAssetThatDoesNotExist() {

            AssetCreateRequestDto payload = getAssetCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> walletService.addAssetToWallet(TOKEN, payload));
        }

        @Test
        @DisplayName("Should not be able to add an existing asset to wallet")
        void shouldNotBeAbleToAddAnExistingAssetToWallet() {

            // ARRANGE
            AssetCreateRequestDto payload = getAssetCreateRequestDto();
            WalletEntity.Asset asset = new WalletEntity.Asset(payload.assetName(), payload.quotaAmount(), new HashSet<>(), new HashSet<>());
            WalletEntity existingWallet = new WalletEntity();
            existingWallet.setUserId(USER_ID);
            existingWallet.getAsset().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingWallet));

            // ACT & ASSERT
            assertThrows(ConflictException.class, () -> walletService.addAssetToWallet(TOKEN, payload));
        }

        private static AssetCreateRequestDto getAssetCreateRequestDto() {
            return new AssetCreateRequestDto(
                    ASSET_NAME,
                    ASSET_TYPE,
                    100
            );
        }
    }

    @Nested
    class addPurchaseToAsset {

        @Test
        @DisplayName("Should be able to add purchase to asset")
        void shouldBeAbleToAddPurchaseToAsset() {

            PurchasesInfoRequestDto payload = getPurchasesInfoRequestDto();

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            asset.setAssetName(payload.assetName());
            wallet.getAsset().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.addPurchaseToAsset(TOKEN, payload);

            String message = "A compra do seu ativo " + payload.assetName() + " foi cadastrada com sucesso." ;

            verify(walletRepository, times(1)).addPurchaseToAssetByUserIdAndAssetName(
                    eq(USER_ID), eq("ABCD11"), any(WalletEntity.Asset.PurchasesInfo.class), eq(10)
            );
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset that does not exist")
        void shouldNotBeAbleToAddPurchaseToAssetThatDoesNotExist() {

            PurchasesInfoRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));

            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset without having wallet created")
        void shouldNotBeAbleToAddPurchaseToAssetWithoutHavingWalletCreated() {

            PurchasesInfoRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));

            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset that is not in wallet")
        void shouldNotBeAbleToAddPurchaseToAssetThatIsNotInWallet() {

            PurchasesInfoRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            ForbiddenException exception = assertThrows(ForbiddenException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));

            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        private static PurchasesInfoRequestDto getPurchasesInfoRequestDto() {
            String dateTimeString = "2024-10-06T10:00:00.000Z";

            PurchasesInfoRequestDto payload = new PurchasesInfoRequestDto(
                    ASSET_NAME,
                    ASSET_TYPE,
                    10,
                    BigDecimal.valueOf(25.20),
                    Instant.parse(dateTimeString)
            );
            return payload;
        }
    }

    @Nested
    class addSaleToAsset {

        @Test
        @DisplayName("Should be able to add sale to asset")
        void shouldBeAbleToAddSaleToAsset() {

            SalesInfoRequestDto payload = getSalesInfoRequestDto();

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            asset.setAssetName(payload.assetName());
            asset.setQuotaAmount(100);
            wallet.getAsset().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.addSaleToAsset(TOKEN, payload);

            String message = "A venda do seu ativo " + payload.assetName() + " foi cadastrada com sucesso." ;

            verify(walletRepository, times(1)).addSaleToAssetByUserIdAndAssetName(
                    eq(USER_ID), eq("ABCD11"), any(WalletEntity.Asset.SalesInfo.class), eq(10)
            );
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to add sale to asset that does not exist")
        void shouldNotBeAbleToAddSaleToAssetThatDoesNotExist() {

            SalesInfoRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addSaleToAsset(TOKEN, payload));

            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset without having wallet created")
        void shouldNotBeAbleToAddSaleToAssetWithoutHavingWalletCreated() {

            SalesInfoRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addSaleToAsset(TOKEN, payload));

            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset that is not in wallet")
        void shouldNotBeAbleToAddSaleToAssetThatIsNotInWallet() {

            SalesInfoRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            ForbiddenException exception = assertThrows(ForbiddenException.class, () -> walletService.addSaleToAsset(TOKEN, payload));

            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset when quota amount is less than sale amount")
        void shoulNotBeAbleToAddPurchaseToAssetWhenQuotaAmountIsLessThanSaleAmount() {

            SalesInfoRequestDto payload = getSalesInfoRequestDto();

            WalletEntity.Asset asset = new WalletEntity.Asset(ASSET_NAME, 5, new HashSet<>(), new HashSet<>());
            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(USER_ID);
            wallet.getAsset().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(true);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.addSaleToAsset(TOKEN, payload));

            assertEquals("A quantidade de cota do ativo não pode ser negativa.", exception.getMessage());
        }

        private static SalesInfoRequestDto getSalesInfoRequestDto() {
            String dateTimeString = "2024-10-06T10:00:00.000Z";

            SalesInfoRequestDto payload = new SalesInfoRequestDto(
                    ASSET_NAME,
                    ASSET_TYPE,
                    10,
                    BigDecimal.valueOf(25.20),
                    Instant.parse(dateTimeString)
            );
            return payload;
        }
    }

}