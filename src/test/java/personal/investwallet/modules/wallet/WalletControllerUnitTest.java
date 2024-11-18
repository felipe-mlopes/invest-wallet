package personal.investwallet.modules.wallet;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import personal.investwallet.modules.wallet.dto.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletControllerUnitTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private Validator validator;

    private final String TOKEN = "valid-token";

    @BeforeEach
    void setUp() {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    class Create {

        @Test
        @DisplayName("Should be able to create wallet and add asset to it with valid payload")
        void shouldBeAbleToCreateWalletAndAddAssetToItWithValidPayload() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("ABCD11");

            ResponseEntity<WallerSuccessResponseDto> response = walletController.create(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).addAssetToWallet(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset to it with empty payload")
        void shouldNotBeAbleToCreateWalletAndAddAssetToItWithEmptyPayload() {

            CreateAssetRequestDto invalidPayload = new CreateAssetRequestDto("");

            var violations = validator.validate(invalidPayload);
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset to it with invalid payload")
        void shouldNotBeAbleToCreateWalletAndAddAssetToItWithInvalidPayload() {

            CreateAssetRequestDto invalidPayload = new CreateAssetRequestDto("AB11");

            var violations = validator.validate(invalidPayload);

            assertEquals(1, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
        }
    }

    @Nested
    class AddPurchase {

        @Test
        @DisplayName("Should be able to add purchase of asset to wallet with valid payload")
        void shouldBeAbleToAddPurchaseOfAssetToWalletWithValidPayload() {

            AddPurchaseRequestDto payload = new AddPurchaseRequestDto(
                    "ABCD11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            ResponseEntity<WallerSuccessResponseDto> response = walletController.addPurchase(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).addPurchaseToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add purchase of asset to wallet with empty asset name in payload")
        void shouldNotBeAbleToAddPurchaseOfAssetToWalletWithEmptyAssetNameInPayload() {

            AddPurchaseRequestDto invalidPayload = new AddPurchaseRequestDto(
                    "",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to add purchase of asset to wallet with invalid payload")
        void shouldNotBeAbleToAddPurchaseOfAssetToWalletWithInvalidPayload() {

            AddPurchaseRequestDto invalidPayload = new AddPurchaseRequestDto(
                    "AB11",
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertEquals(4, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O número de cotas compradas deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O preço da compra deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("A data da compra deve ser anterior a data corrente")));
        }
    }

    @Nested
    class AddManyPurchasesByCSV {

        @Test
        @DisplayName("Should be able to add many purchases of asset to wallet by using file")
        void shouldBeAbleToAddManyPurchasesOfAssetToWalletByUsingFile() {

            String csvContent = "asset_name,date,amount,price,quota_value\n" +
                    "ABCD11,01/01/2024,10,28.51,28.51";

            MultipartFile file = new MockMultipartFile(
                    "purchases",
                    "purchases.csv",
                    "text/csv",
                    csvContent.getBytes());

            ResponseEntity<WallerSuccessResponseDto> response = walletController.addManyPurchasesByCSV(TOKEN, file);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).addManyPurchasesToAssetByFile(TOKEN, file);
        }
    }

    @Nested
    class UpdatePurchase {

        @Test
        @DisplayName("Should be able to update purchase of asset in wallet with valid payload")
        void shouldBeAbleToUpdatePurchaseOfAssetInWalletWithValidPayload() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            ResponseEntity<WallerSuccessResponseDto> response = walletController.updatePurchase(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload);
        }

        @Test
        @DisplayName("Should not be able to update purchase of asset in wallet create with invalid payload")
        void shouldNotBeAbleToUpdatePurchaseOfAssetInWalletWithInvalidPayload() {

            UpdatePurchaseRequestDto invalidPayload = new UpdatePurchaseRequestDto(
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O número de cotas compradas deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O preço da compra deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("A data da compra não pode ser no futuro")));
        }
    }

    @Nested
    class RemovePurchase {

        @Test
        @DisplayName("Should be able to remove purchase of asset from wallet by purchaseId")
        void shouldBeAbleToRemovePurchaseOfAssetFromWalletByPurchaseId() {

            ResponseEntity<WallerSuccessResponseDto> response = walletController.removePurchase(
                    TOKEN, "ABCD11", "purchaseId");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).removePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId");
        }
    }

    @Nested
    class AddSale {

        @Test
        @DisplayName("Should be able to add sale of asset from wallet with valid payload")
        void shouldBeAbleToAddSaleOfAssetFromWalletWithValidPayload() {

            AddSaleRequestDto payload = new AddSaleRequestDto(
                    "ABCD11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            ResponseEntity<WallerSuccessResponseDto> response = walletController.addSale(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale of asset from wallet with empty asset name in payload")
        void shouldNotBeAbleToAddSaleOfAssetFromWalletWithEmptyAssetNameInPayload() {

            AddSaleRequestDto invalidPayload = new AddSaleRequestDto(
                    "",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to add sale of asset from wallet create with invalid payload")
        void shouldNotBeAbleToAddSaleOfAssetFromWalletWithInvalidPayload() {

            AddSaleRequestDto invalidPayload = new AddSaleRequestDto(
                    "AB11",
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertEquals(4, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O número de cotas vendidas deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O preço da venda deve ser maior que zero")));
            assertTrue(
                    violations.stream().anyMatch(v -> v.getMessage().equals("A data da venda não pode ser no futuro")));
        }
    }

    @Nested
    class AddManySalesByCSV {

        @Test
        @DisplayName("Should be able to add many sales of asset to wallet by using file")
        void shouldBeAbleToAddManySalesOfAssetToWalletByUsingFile() {

            String csvContent = "asset_name,date,amount,price,quota_value\n" +
                    "ABCD11,01/01/2024,10,28.51,28.51";

            MultipartFile file = new MockMultipartFile(
                    "sales",
                    "sales.csv",
                    "text/csv",
                    csvContent.getBytes());

            ResponseEntity<WallerSuccessResponseDto> response = walletController.addManySalesByCSV(TOKEN, file);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).addManySalesToAssetByFile(TOKEN, file);
        }
    }

    @Nested
    class UpdateSale {

        @Test
        @DisplayName("Should be able to update sale of asset from wallet with valid payload")
        void shouldBeAbleToUpdateSaleOfAssetFromWalletWithValidPayload() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1)));

            ResponseEntity<WallerSuccessResponseDto> response = walletController.updateSale(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload);
        }

        @Test
        @DisplayName("Should not be able to update sale of asset from wallet create with invalid payload")
        void shouldNotBeAbleToUpdateSaleOfAssetFromWalletWithInvalidPayload() {

            UpdateSaleRequestDto invalidPayload = new UpdateSaleRequestDto(
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1)));

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O número de cotas vendidas deve ser maior que zero")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O preço da venda deve ser maior que zero")));
            assertTrue(
                    violations.stream().anyMatch(v -> v.getMessage().equals("A data da venda não pode ser no futuro")));
        }
    }

    @Nested
    class RemoveSale {

        @Test
        @DisplayName("Should be able to remove sale of asset from wallet by saleId")
        void shouldBeAbleToRemoveSaleOfAssetFromWalletBySaleId() {

            ResponseEntity<WallerSuccessResponseDto> response = walletController.removeSale(
                    TOKEN, "ABCD11", "saleId");

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(walletService, times(1)).removeSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId");
        }
    }
}
