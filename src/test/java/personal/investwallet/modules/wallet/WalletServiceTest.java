package personal.investwallet.modules.wallet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.exceptions.BadRequestException;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.asset.AssetService;
import personal.investwallet.modules.wallet.dto.*;
import personal.investwallet.security.TokenService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private WalletService walletService;

    public static final String TOKEN = "validToken";
    public static final String USER_ID = "user1234";
    public static final String ASSET_NAME = "ABCD11";
    public static final String ASSET_TYPE = "fundos-imobiliarios";
    public static final String PURCHASE_ID = "purchase123";
    public static final String SALE_ID = "sale123";

    @Nested
    class addAssetToWallet {

        @Test
        @DisplayName("Should be able to create a new wallet by adding an asset")
        void shouldBeAbleToCreateNewWalletByAddingAnAsset() {

            CreateAssetRequestDto payload = getAssetsCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            String result = walletService.addAssetToWallet(TOKEN, payload);

            verify(walletRepository, times(1)).save(any(WalletEntity.class));
            assertEquals("Uma nova carteira foi criada e o ativo ABCD11 foi adicionado.", result);
        }

        @Test
        @DisplayName("Should be able to add a new asset to wallet")
        void shouldBeAbleToAddNewAssetToWallet() {

            CreateAssetRequestDto payload = getAssetsCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            String result = walletService.addAssetToWallet(TOKEN, payload);

            verify(walletRepository,times(1)).addNewAssetByUserId(eq(USER_ID), eq(payload.assetName()),any(WalletEntity.Asset.class));
            assertEquals("O ativo ABCD11 foi adicionado à carteira com sucesso.", result);
        }

        @Test
        @DisplayName("Should not be able to create a new wallet or add a new asset with an asset that does not exist")
        void shouldNotBeAbleToCreateNewWalletOrAddNewAssetWithAnAssetThatDoesNotExist() {

            CreateAssetRequestDto payload = getAssetsCreateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addAssetToWallet(TOKEN, payload));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add an existing asset to wallet")
        void shouldNotBeAbleToAddAnExistingAssetToWallet() {

            CreateAssetRequestDto payload = getAssetsCreateRequestDto();
            WalletEntity.Asset asset = new WalletEntity.Asset(payload.assetName(), 0, new ArrayList<>(), new ArrayList<>());
            WalletEntity existingWallet = new WalletEntity();
            existingWallet.setUserId(USER_ID);
            existingWallet.getAssets().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingWallet));

            ConflictException exception = assertThrows(ConflictException.class, () -> walletService.addAssetToWallet(TOKEN, payload));
            assertEquals("O ativo informado já existe na carteira.", exception.getMessage());
        }

        private static CreateAssetRequestDto getAssetsCreateRequestDto() {
            return new CreateAssetRequestDto(
                    ASSET_NAME
            );
        }
    }

    @Nested
    class addPurchaseToAsset {

        @Test
        @DisplayName("Should be able to add purchase to asset")
        void shouldBeAbleToAddPurchaseToAsset() {

            AddPurchaseRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset(ASSET_NAME, 10, new ArrayList<>(), new ArrayList<>());
            wallet.getAssets().put(ASSET_NAME, asset);

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

            AddPurchaseRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset without having wallet created")
        void shouldNotBeAbleToAddPurchaseToAssetWithoutHavingWalletCreated() {

            AddPurchaseRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset that is not in wallet")
        void shouldNotBeAbleToAddPurchaseToAssetThatIsNotInWallet() {

            AddPurchaseRequestDto payload = getPurchasesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(payload.assetName())).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.addPurchaseToAsset(TOKEN, payload));
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        private static AddPurchaseRequestDto getPurchasesInfoRequestDto() {
            String dateTimeString = "2024-10-06T10:00:00.000Z";

            return new AddPurchaseRequestDto(
                    ASSET_NAME,
                    10,
                    BigDecimal.valueOf(25.20),
                    Instant.parse(dateTimeString)
            );
        }
    }

    @Nested
    class addAllPurchaseToAssetByFile {

        @Test
        @DisplayName("Should be able to create new wallet and add all purchases to asset by file")
        void shouldBeAbleToCreateNewWalletAndAddAllPurchasesToAssetByFile() {

            String csvContent = "asset_name,date,amount,price,quota_value\n" +
                    "ABCD11,01/01/2024,100,28.50,28.50";

            MultipartFile file = new MockMultipartFile(
                    "file",
                    "purchase.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllPurchasesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(100, savedAsset.getQuotaAmount());
            assertEquals(1, savedAsset.getPurchasesInfo().size());

            WalletEntity.Asset.PurchasesInfo savedPurchase = savedAsset.getPurchasesInfo().get(0);
            assertEquals(100, savedPurchase.getPurchaseAmount());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchasePrice());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchaseQuotaValue());

            String message = "Uma carteira foi criada e os registros de vendas foram cadastrados com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to add all purchases to asset by file in wallet already created")
        void shouldBeAbleToAddAllPurchasesToAssetByFileInWalletAlreadyCreated() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();

            MultipartFile file = getMultipartFile();

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllPurchasesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(30, savedAsset.getQuotaAmount());
            assertEquals(1, savedAsset.getPurchasesInfo().size());

            WalletEntity.Asset.PurchasesInfo savedPurchase = savedAsset.getPurchasesInfo().get(0);
            assertEquals(10, savedPurchase.getPurchaseAmount());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchasePrice());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchaseQuotaValue());

            String message = "Os registros de compras foram cadastrados na carteira com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to add all purchases to asset by file and skip duplicate purchase by date")
        void shouldBeAbleToAddAllPurchasesToAssetByFileInWalletAndSkipDuplicatePurchaseByDate() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();

            String csvContent = "asset_name,date,amount,price,quota_value\n" +
                    "ABCD11,01/01/2024,100,2850,2850\n" +
                    "ABCD11,01/01/2024,200,2950,2950";

            MultipartFile file = new MockMultipartFile(
                    "file",
                    "purchases.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllPurchasesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(120, savedAsset.getQuotaAmount());
            assertEquals(1, savedAsset.getPurchasesInfo().size());

            WalletEntity.Asset.PurchasesInfo savedPurchase = savedAsset.getPurchasesInfo().get(0);
            assertEquals(100, savedPurchase.getPurchaseAmount());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchasePrice());
            assertEquals(new BigDecimal(2850), savedPurchase.getPurchaseQuotaValue());

            String message = "Os registros de compras foram cadastrados na carteira com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to add all purchases to asset by file if asset does not exist")
        void shouldNotBeAbleToAddAllPurchasesToAssetByFileIfAssetDoesNotExist() {

            MultipartFile file = getMultipartFile();

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addAllPurchasesToAssetByFile(
                    TOKEN, file
            ));
            assertEquals("O ativo " + ASSET_NAME + " informado não existe.", exception.getMessage());
        }
    }

    @Nested
    class updatePurchaseToAssetByPurchaseId {

        @Test
        @DisplayName("Should be able to update asset's purchase info by purchaseId with purchase amount different from the previous one")
        void shouldBeAbleToUpdateAssetsPurchaseInfoByPurchaseIdWithPurchaseAmountDifferentFromPreviousOne() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            doAnswer(invocationOnMock -> {
               int restoreAmount = invocationOnMock.getArgument(2);
               asset.setQuotaAmount(asset.getQuotaAmount() + restoreAmount);
               return null;
            }).when(walletRepository).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), anyInt()
            );

            doAnswer(invocation -> {
                int newAmount = invocation.getArgument(3);
                asset.setQuotaAmount(asset.getQuotaAmount() + newAmount);
                return null;
            }).when(walletRepository).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), anyInt()
            );

            String result = walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID,
                    payload
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), eq(-5)
            );
            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(25, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's purchase info by purchaseId with the same purchase amount as the previous one")
        void shouldBeAbleToUpdateAssetsPurchaseInfoByPurchaseIdWithSamePurchaseAmountAsPreviousOne() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID,
                    payload
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's purchase info by purchaseId with purchase amount null")
        void shouldBeAbleToUpdateAssetsPurchaseInfoByPurchaseIdWithPurchaseAmountNull() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    null,
                    BigDecimal.valueOf(38.14),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ArgumentCaptor<List<WalletEntity.Asset.PurchasesInfo>> purchasesCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Integer> quotaCaptor = ArgumentCaptor.forClass(Integer.class);

            String result = walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID,
                    payload
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), purchasesCaptor.capture(), quotaCaptor.capture()
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's purchase info by purchaseId with purchase price null")
        void shouldBeAbleToUpdateAssetsPurchaseInfoByPurchaseIdWithPurchasePriceNull() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    15,
                    null,
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            doAnswer(invocationOnMock -> {
                int restoreAmount = invocationOnMock.getArgument(2);
                asset.setQuotaAmount(asset.getQuotaAmount() + restoreAmount);
                return null;
            }).when(walletRepository).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), anyInt()
            );

            doAnswer(invocation -> {
                int newAmount = invocation.getArgument(3);
                asset.setQuotaAmount(asset.getQuotaAmount() + newAmount);
                return null;
            }).when(walletRepository).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), anyInt()
            );

            String result = walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID,
                    payload
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(15)
            );

            assertEquals(30, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's purchase info by purchaseId with purchase date null")
        void shouldBeAbleToUpdateAssetsPurchaseInfoByPurchaseIdWithPurchaseDateNull() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    10,
                    BigDecimal.valueOf(38.14),
                    null
            );

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID,
                    payload
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info by purchaseId without sending updated info")
        void shouldNotBeAbleToUpdateInfoToAssetsByPurchaseIdWithoutSendingUpdatedInfo() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(null, null, null);

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Não há informações de compra para serem atualizadas.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info if purchaseId does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetIfPurchaseDoesNotExist() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            WalletEntity.Asset asset = new WalletEntity.Asset(
                    ASSET_NAME,
                    20,
                    List.of(new WalletEntity.Asset.PurchasesInfo(
                            "purchaseId",
                            10,
                            BigDecimal.valueOf(25.20),
                            BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(10)),
                            Instant.now()
                    )),
                    new ArrayList<>()
            );
            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(USER_ID);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Não existe compra com o ID informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info by purchaseId if wallet does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetByPurchaseIdIfWalletDoesNotExist() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Nenhuma carteira foi localizada para esse usuário.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info by purchaseId that does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetByPurchaseIdThatAssetDoesNotExist() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info by purchaseId without having wallet created")
        void shouldNotBeAbleToUpdateInfoToAssetByPurchaseIdWithoutHavingWalletCreated() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,  () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's purchase info by purchaseId that is not in wallet")
        void shouldNotBeAbleToUpdateInfoToAssetByPurchaseIdThatIsNotInWallet() {

            UpdatePurchaseRequestDto payload = getPurchaseOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        private static UpdatePurchaseRequestDto getPurchaseOnUpdateRequestDto() {
            return new UpdatePurchaseRequestDto(
                    10,
                    BigDecimal.valueOf(38.14),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );
        }
    }

    @Nested
    class removePurchaseToAssetByPurchaseId {

        @Test
        @DisplayName("Should be able to remove asset's purchase by purchaseId")
        void shouldBeAbleToRemovePurchaseToAssetByPurchaseId() {

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            ArgumentCaptor<List<WalletEntity.Asset.PurchasesInfo>> purchasesCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Integer> quotaCaptor = ArgumentCaptor.forClass(Integer.class);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            doNothing().when(walletRepository).updatePurchaseInAssetByPurchaseId(
                    anyString(),
                    anyString(),
                    anyList(),
                    anyInt()
            );

            String result = walletService.removePurchaseToAssetByPurchaseId(
                    TOKEN,
                    ASSET_NAME,
                    PURCHASE_ID
            );

            String message = "A compra " + PURCHASE_ID + " do ativo " + ASSET_NAME + " foi removida com sucesso." ;

            verify(walletRepository, times(1)).updatePurchaseInAssetByPurchaseId(
                        eq(USER_ID),
                        eq(ASSET_NAME),
                        purchasesCaptor.capture(),
                        quotaCaptor.capture()
            );

            assertEquals(15, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to remove asset's purchase by purchaseId if the user does not have a wallet")
        void shouldNotBeAbleToRemovePurchaseToAssetByPurchaseIdIfTheUserDoesNotHaveAWallet() {

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString()
            ));
            assertEquals("Nenhuma carteira foi localizada para esse usuário.", exception.getMessage());

        }

        @Test
        @DisplayName("Should not be able to remove asset's purchase info by purchaseId that does not exist")
        void shouldNotBeAbleToRemovePurchaseInfoToAssetByPurchaseIdThatAssetDoesNotExist() {

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, PURCHASE_ID
            ));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's purchase info by purchaseId without having wallet created")
        void shouldNotBeAbleToRemovePurchaseInfoToAssetByPurchaseIdWithoutHavingWalletCreated() {

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, PURCHASE_ID
            ));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's purchase by purchaseId if the asset is not in wallet")
        void shouldNotBeAbleToRemovePurchaseInfoToAssetByPurchaseIdIfTheAssetIsNotInWallet() {

            WalletEntity wallet = createEmptyWallet();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.removePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, PURCHASE_ID)
            );
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's purchase by purchaseId that is not in wallet")
        void shouldNotBeAbleToRemovePurchaseInfoToAssetByPurchaseIdThatIsNotInWallet() {

            WalletEntity wallet = createWalletWithAssetAndPurchaseInfo();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removePurchaseToAssetByPurchaseId(
                    TOKEN, ASSET_NAME, anyString()
            ));
            assertEquals("Compra com o ID fornecido não encontrada.", exception.getMessage());
        }
    }

    @Nested
    class addSaleToAsset {

        @Test
        @DisplayName("Should be able to add sale to asset")
        void shouldBeAbleToAddSaleToAsset() {

            AddSaleRequestDto payload = getSalesInfoRequestDto();

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            asset.setAssetName(payload.assetName());
            asset.setQuotaAmount(100);
            wallet.getAssets().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.addSaleToAsset(TOKEN, payload);

            String message = "A venda do seu ativo " + payload.assetName() + " foi cadastrada com sucesso." ;

            verify(walletRepository, times(1)).addSaleToAssetByUserIdAndAssetName(
                    eq(USER_ID), eq("ABCD11"), any(WalletEntity.Asset.SalesInfo.class), eq(-10)
            );
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to add sale to asset that does not exist")
        void shouldNotBeAbleToAddSaleToAssetThatDoesNotExist() {

            AddSaleRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addSaleToAsset(TOKEN, payload));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset without having wallet created")
        void shouldNotBeAbleToAddSaleToAssetWithoutHavingWalletCreated() {

            AddSaleRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addSaleToAsset(TOKEN, payload));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset that is not in wallet")
        void shouldNotBeAbleToAddSaleToAssetThatIsNotInWallet() {

            AddSaleRequestDto payload = getSalesInfoRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.addSaleToAsset(TOKEN, payload));
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add purchase to asset when quota amount is less than sale amount")
        void shouldNotBeAbleToAddPurchaseToAssetWhenQuotaAmountIsLessThanSaleAmount() {

            AddSaleRequestDto payload = getSalesInfoRequestDto();

            WalletEntity.Asset asset = new WalletEntity.Asset(ASSET_NAME, 5, new ArrayList<>(), new ArrayList<>());
            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(USER_ID);
            wallet.getAssets().put(asset.getAssetName(), asset);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.addSaleToAsset(TOKEN, payload));
            assertEquals("A quantidade de cota do ativo não pode ser negativa.", exception.getMessage());
        }

        private static AddSaleRequestDto getSalesInfoRequestDto() {
            String dateTimeString = "2024-10-06T10:00:00.000Z";

            return new AddSaleRequestDto(
                    ASSET_NAME,
                    10,
                    BigDecimal.valueOf(25.20),
                    Instant.parse(dateTimeString)
            );
        }

}

    @Nested
    class addAllSaleToAssetByFile {

        @Test
        @DisplayName("Should be able to create new wallet and add all sales to asset by file")
        void shouldBeAbleToCreateNewWalletAndAddAllSalesToAssetByFile() {

            MultipartFile file = getMultipartFile();

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllSalesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(10, savedAsset.getQuotaAmount());
            assertEquals(1, savedAsset.getSalesInfo().size());

            WalletEntity.Asset.SalesInfo savedSale = savedAsset.getSalesInfo().get(0);
            assertEquals(10, savedSale.getSaleAmount());
            assertEquals(new BigDecimal(2850), savedSale.getSalePrice());
            assertEquals(new BigDecimal(2850), savedSale.getSaleQuotaValue());

            String message = "Uma carteira foi criada e os registros de vendas foram cadastrados com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to add all sales to asset by file in wallet already created")
        void shouldBeAbleToAddAllSalesToAssetByFileInWalletAlreadyCreated() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();

            MultipartFile file = getMultipartFile();

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllSalesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(10, savedAsset.getQuotaAmount());
            assertEquals(2, savedAsset.getSalesInfo().size());

            WalletEntity.Asset.SalesInfo savedSale = savedAsset.getSalesInfo().get(1);
            assertEquals(10, savedSale.getSaleAmount());
            assertEquals(new BigDecimal(2850), savedSale.getSalePrice());
            assertEquals(new BigDecimal(2850), savedSale.getSaleQuotaValue());

            String message = "Os registros de vendas foram cadastrados na carteira com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to add all sales to asset by file and skip duplicate purchase by date")
        void shouldBeAbleToAddAllSalesToAssetByFileInWalletAndSkipDuplicateSaleByDate() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();

            String csvContent = "asset_name,date,amount,price,quota_value\n" +
                    "ABCD11,01/01/2024,10,2850,2850\n" +
                    "ABCD11,01/01/2024,15,2950,2950";

            MultipartFile file = new MockMultipartFile(
                    "file",
                    "sales.csv",
                    "text/csv",
                    csvContent.getBytes()
            );

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ArgumentCaptor<WalletEntity> walletCaptor = ArgumentCaptor.forClass(WalletEntity.class);
            when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArguments()[0]);

            String result = walletService.addAllSalesToAssetByFile(TOKEN, file);

            verify(walletRepository).save(walletCaptor.capture());

            WalletEntity savedWallet = walletCaptor.getValue();
            WalletEntity.Asset savedAsset = savedWallet.getAssets().get(ASSET_NAME);

            assertNotNull(savedAsset);
            assertEquals(USER_ID, savedWallet.getUserId());
            assertEquals(ASSET_NAME, savedAsset.getAssetName());
            assertEquals(10, savedAsset.getQuotaAmount());
            assertEquals(2, savedAsset.getSalesInfo().size());

            WalletEntity.Asset.SalesInfo savedSale = savedAsset.getSalesInfo().get(1);
            assertEquals(10, savedSale.getSaleAmount());
            assertEquals(new BigDecimal(2850), savedSale.getSalePrice());
            assertEquals(new BigDecimal(2850), savedSale.getSaleQuotaValue());

            String message = "Os registros de vendas foram cadastrados na carteira com sucesso." ;

            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to add all sales to asset by file if asset does not exist")
        void shouldNotBeAbleToAddAllPurchasesToAssetByFileIfAssetDoesNotExist() {

            MultipartFile file = getMultipartFile();

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.addAllPurchasesToAssetByFile(
                    TOKEN, file
            ));
            assertEquals("O ativo " + ASSET_NAME + " informado não existe.", exception.getMessage());
        }
    }

    @Nested
    class updateSaleToAssetBySaleId {

        @Test
        @DisplayName("Should be able to update asset's sale info by saleId with sale amount different from the previous one")
        void shouldBeAbleToUpdateAssetsSaleInfoBySaleIdWithSaleAmountDifferentFromPreviousOne() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();

            WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                    "sale123",
                    5,
                    BigDecimal.valueOf(25.78),
                    BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(5)),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            salesInfo.add(sale);
            asset.setAssetName(ASSET_NAME);
            asset.setQuotaAmount(20);
            asset.setSalesInfo(salesInfo);

            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            doAnswer(invocationOnMock -> {
                int restoreAmount = invocationOnMock.getArgument(2);
                asset.setQuotaAmount(asset.getQuotaAmount() + restoreAmount);
                return null;
            }).when(walletRepository).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), anyInt()
            );

            doAnswer(invocation -> {
                int newAmount = invocation.getArgument(3);
                asset.setQuotaAmount(asset.getQuotaAmount() - newAmount);
                return null;
            }).when(walletRepository).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), anyInt()
            );

            String result = walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    sale.getSaleId(),
                    payload
            );

            String message = "A venda " + sale.getSaleId() + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), eq(5)
            );
            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(15, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's sale info by saleId with the same sale amount as the previous one")
        void shouldBeAbleToUpdateAssetsSaleInfoBySaleIdWithSameSaleAmountAsPreviousOne() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();

            WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                    "sale123",
                    10,
                    BigDecimal.valueOf(25.78),
                    BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            salesInfo.add(sale);
            asset.setAssetName(ASSET_NAME);
            asset.setQuotaAmount(20);
            asset.setSalesInfo(salesInfo);

            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    sale.getSaleId(),
                    payload
            );

            String message = "A venda " + sale.getSaleId() + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's sale info by saleId with sale amount null")
        void shouldBeAbleToUpdateAssetsSaleInfoBySaleIdWithSaleAmountNull() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                    null,
                    BigDecimal.valueOf(38.14),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();

            WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                    "sale123",
                    10,
                    BigDecimal.valueOf(25.78),
                    BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            salesInfo.add(sale);
            asset.setAssetName(ASSET_NAME);
            asset.setQuotaAmount(20);
            asset.setSalesInfo(salesInfo);

            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    sale.getSaleId(),
                    payload
            );

            String message = "A venda " + sale.getSaleId() + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to update asset's sale info by saleId with sale price null")
        void shouldBeAbleToUpdateAssetsSaleInfoBySaleIdWithSalePriceNull() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                    15,
                    null,
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();

            WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                    "sale123",
                    5,
                    BigDecimal.valueOf(25.78),
                    BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(5)),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            salesInfo.add(sale);
            asset.setAssetName(ASSET_NAME);
            asset.setQuotaAmount(20);
            asset.setSalesInfo(salesInfo);

            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            doAnswer(invocationOnMock -> {
                int restoreAmount = invocationOnMock.getArgument(2);
                asset.setQuotaAmount(asset.getQuotaAmount() + restoreAmount);
                return null;
            }).when(walletRepository).restoreAmountOfQuotasInAsset(
                    eq(USER_ID), eq(ASSET_NAME), anyInt()
            );

            doAnswer(invocation -> {
                int newAmount = invocation.getArgument(3);
                asset.setQuotaAmount(asset.getQuotaAmount() - newAmount);
                return null;
            }).when(walletRepository).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), anyInt()
            );

            String result = walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    sale.getSaleId(),
                    payload
            );

            String message = "A venda " + sale.getSaleId() + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(15)
            );

            assertEquals(10, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should be able to sale asset's sale info by saleId with sale date null")
        void shouldBeAbleToUpdateAssetsSaleInfoBySaleIdWithSaleDateNull() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                    10,
                    BigDecimal.valueOf(38.14),
                    null
            );

            WalletEntity wallet = new WalletEntity();
            WalletEntity.Asset asset = new WalletEntity.Asset();
            List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();

            WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                    "purchase123",
                    10,
                    BigDecimal.valueOf(25.78),
                    BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );

            salesInfo.add(sale);
            asset.setAssetName(ASSET_NAME);
            asset.setQuotaAmount(20);
            asset.setSalesInfo(salesInfo);

            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            String result = walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    sale.getSaleId(),
                    payload
            );

            String message = "A venda " + sale.getSaleId() + " do ativo " + ASSET_NAME + " foi atualizada com sucesso." ;

            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID), eq(ASSET_NAME), anyList(), eq(10)
            );

            assertEquals(20, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to update asset's sale info by saleId without sending updated info")
        void shouldNotBeAbleToUpdateInfoToAssetsBySaleIdWithoutSendingUpdatedInfo() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(null, null, null);

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Não há informações de venda para serem atualizadas.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's sale info if saleId does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetIfSaleDoesNotExist() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            WalletEntity.Asset asset = new WalletEntity.Asset(
                    ASSET_NAME,
                    20,
                    new ArrayList<>(),
                    List.of(new WalletEntity.Asset.SalesInfo(
                            "saleId",
                            10,
                            BigDecimal.valueOf(25.20),
                            BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(10)),
                            Instant.now()
                    ))
            );
            Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
            assetMap.put(ASSET_NAME, asset);

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(USER_ID);
            wallet.setAssets(assetMap);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Não existe venda com o ID informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update asset's sale info by saleId if wallet does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetBySaleIdIfWalletDoesNotExist() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Nenhuma carteira foi localizada para esse usuário.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to update info sale to asset that does not exist")
        void shouldNotBeAbleToUpdateInfoToAssetBySaleIdThatAssetDoesNotExist() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset without having wallet created")
        void shouldNotBeAbleToUpdateInfoToAssetBySaleIdWithoutHavingWalletCreated() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,  () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to add sale to asset that is not in wallet")
        void shouldNotBeAbleToUpdateInfoToAssetBySaleIdThatIsNotInWallet() {

            UpdateSaleRequestDto payload = getSaleOnUpdateRequestDto();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new WalletEntity()));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.updateSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString(), payload
            ));
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        private static UpdateSaleRequestDto getSaleOnUpdateRequestDto() {
            return new UpdateSaleRequestDto(
                    10,
                    BigDecimal.valueOf(38.14),
                    Instant.now().minus(30, ChronoUnit.MINUTES)
            );
        }
    }

    @Nested
    class removeSaleToAssetBySaleId {

        @Test
        @DisplayName("Should be able to remove asset's sale by saleId")
        void shouldBeAbleToRemoveSaleToAssetBySaleId() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();
            WalletEntity.Asset asset = wallet.getAssets().get(ASSET_NAME);

            ArgumentCaptor<List<WalletEntity.Asset.SalesInfo>> salesCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Integer> quotaCaptor = ArgumentCaptor.forClass(Integer.class);

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            doNothing().when(walletRepository).updateSaleInAssetBySaleId(
                    anyString(),
                    anyString(),
                    anyList(),
                    anyInt()
            );

            String result = walletService.removeSaleToAssetBySaleId(
                    TOKEN,
                    ASSET_NAME,
                    SALE_ID
            );

            String message = "A venda " + SALE_ID + " do ativo " + ASSET_NAME + " foi removida com sucesso." ;

            verify(walletRepository, times(1)).updateSaleInAssetBySaleId(
                    eq(USER_ID),
                    eq(ASSET_NAME),
                    salesCaptor.capture(),
                    quotaCaptor.capture()
            );

            assertEquals(25, asset.getQuotaAmount());
            assertEquals(message, result);
        }

        @Test
        @DisplayName("Should not be able to remove asset's sale by saleId if the user does not have a wallet")
        void shouldNotBeAbleToRemoveSaleToAssetBySaleIdIfTheUserDoesNotHaveAWallet() {

            when(tokenService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, UUID.randomUUID().toString()
            ));
            assertEquals("Nenhuma carteira foi localizada para esse usuário.", exception.getMessage());

        }

        @Test
        @DisplayName("Should not be able to remove asset's sale info by saleId that does not exist")
        void shouldNotBeAbleToRemoveSaleInfoToAssetBySaleIdThatAssetDoesNotExist() {

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, SALE_ID
            ));
            assertEquals("O ativo informado não existe.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's sale info by saleId without having wallet created")
        void shouldNotBeAbleToRemoveSaleInfoToAssetBySaleIdWithoutHavingWalletCreated() {

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, SALE_ID
            ));
            assertEquals("Carteira não encontrada para o usuário informado.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's sale by saleId if the asset is not in wallet")
        void shouldNotBeAbleToRemoveSaleInfoToAssetBySaleIdIfTheAssetIsNotInWallet() {

            WalletEntity wallet = createEmptyWallet();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            BadRequestException exception = assertThrows(BadRequestException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, SALE_ID)
            );
            assertEquals("O ativo informado não existe na carteira.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to remove asset's sale by saleId that is not in wallet")
        void shouldNotBeAbleToRemoveSaleInfoToAssetBySaleIdThatIsNotInWallet() {

            WalletEntity wallet = createWalletWithAssetAndSaleInfo();

            when(tokenService.extractUserIdFromToken(anyString())).thenReturn(USER_ID);
            when(walletRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(assetService.getAssetTypeByAssetName(ASSET_NAME)).thenReturn(ASSET_TYPE);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> walletService.removeSaleToAssetBySaleId(
                    TOKEN, ASSET_NAME, anyString()
            ));
            assertEquals("Venda com o ID fornecido não encontrada.", exception.getMessage());
        }
    }

    private WalletEntity createWalletWithAssetAndPurchaseInfo() {

        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(USER_ID);

        Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
        WalletEntity.Asset asset = new WalletEntity.Asset();
        asset.setAssetName(ASSET_NAME);
        asset.setQuotaAmount(20);

        List<WalletEntity.Asset.PurchasesInfo> purchasesInfo = new ArrayList<>();
        WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                PURCHASE_ID,
                5,
                BigDecimal.valueOf(25.78),
                BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(5)),
                Instant.now().minus(30, ChronoUnit.MINUTES)
        );

        purchasesInfo.add(purchase);
        asset.setPurchasesInfo(purchasesInfo);

        assetMap.put(ASSET_NAME, asset);
        wallet.setAssets(assetMap);

        return wallet;
    }

    private WalletEntity createWalletWithAssetAndSaleInfo() {

        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(USER_ID);

        Map<String, WalletEntity.Asset> assetMap = new HashMap<>();
        WalletEntity.Asset asset = new WalletEntity.Asset();
        asset.setAssetName(ASSET_NAME);
        asset.setQuotaAmount(20);

        List<WalletEntity.Asset.SalesInfo> salesInfo = new ArrayList<>();
        WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                SALE_ID,
                5,
                BigDecimal.valueOf(25.78),
                BigDecimal.valueOf(25,78).divideToIntegralValue(BigDecimal.valueOf(5)),
                Instant.now().minus(30, ChronoUnit.MINUTES)
        );

        salesInfo.add(sale);
        asset.setSalesInfo(salesInfo);

        assetMap.put(ASSET_NAME, asset);
        wallet.setAssets(assetMap);

        return wallet;
    }

    private WalletEntity createEmptyWallet() {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(USER_ID);
        wallet.setAssets(new HashMap<>());
        return wallet;
    }

    private static MultipartFile getMultipartFile() {
        String csvContent = "asset_name,date,amount,price,quota_value\n" +
                "ABCD11,01/01/2024,10,28.50,28.50";

        return new MockMultipartFile(
                "file",
                "purchase.csv",
                "text/csv",
                csvContent.getBytes()
        );
    }
}
