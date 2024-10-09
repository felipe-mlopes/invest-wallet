package personal.investwallet.modules.wallet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.AssetCreateRequestDto;
import personal.investwallet.modules.webscraper.ScraperService;
import personal.investwallet.security.TokenService;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    public static final String TOKEN = "validToken";
    public static final String USER_ID = "user1234";

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
        @DisplayName("Should not be able to create a new portfolio or add a new asset with an asset that does not exist")
        void shouldNotBeAbleToCreateNewWalletOrAddNewAssetWithAnAssetThatDoesNotExist() {

            // ARRANGE
            AssetCreateRequestDto payload = getAssetCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserEntity()));
            when(scraperService.verifyIfWebsiteIsValid(payload.assetType(), payload.assetName())).thenReturn(false);

            // ACT & ASSERT
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
    }

    @Nested
    class addPurchaseToAsset {
    }

    @Nested
    class addSaleToAsset {
    }

    private static AssetCreateRequestDto getAssetCreateRequestDto() {
        return new AssetCreateRequestDto(
                "ABCD11",
                "fundos-imobiliarios",
                100
        );
    }


}