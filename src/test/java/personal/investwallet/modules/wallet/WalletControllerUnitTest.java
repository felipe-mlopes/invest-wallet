package personal.investwallet.modules.wallet;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jetbrains.annotations.NotNull;
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
import personal.investwallet.exceptions.BadRequestException;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.ResourceNotFoundException;
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
        @DisplayName("Should be able to create wallet and add asset name with valid payload")
        void shouldBeAbleToCreateWalletAndAddAssetNameWithValidPayload() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("ABCD11");

            String message = "O ativo ABCD11 foi adicionado à carteira com sucesso";

            when(walletService.addAssetToWallet(TOKEN, payload)).thenReturn(message);

            ResponseEntity<CreateWalletResponseDto> response = walletController.create(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(walletService, times(1)).addAssetToWallet(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset name when asset name does not exist")
        void shouldNotBeAbleToCreateWalletAndAddAssetNameWhenAssetNameDoesNotExist() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("ABCD11");

            String message = "O ativo informado não existe.";

            when(walletService.addAssetToWallet(TOKEN, payload)).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.create(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addAssetToWallet(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset name when asset already exists")
        void shouldNotBeAbleToCreateWalletAndAddAssetNameWhenAssetAlreadyExists() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("ABCD11");

            String message = "O ativo informado já existe na carteira";

            when(walletService.addAssetToWallet(TOKEN, payload)).thenThrow(
                    new ConflictException(message)
            );

            ConflictException exception = assertThrows(ConflictException.class,
                    () -> walletController.create(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addAssetToWallet(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset name with empty payload")
        void shouldNotBeAbleToCreateWalletAndAddAssetNameWithEmptyPayload() {

            CreateAssetRequestDto invalidPayload = new CreateAssetRequestDto("");

            var violations = validator.validate(invalidPayload);
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to create wallet and add asset name with invalid payload")
        void shouldNotBeAbleToCreateWalletAndAddAssetNameWithInvalidPayload() {

            CreateAssetRequestDto invalidPayload = new CreateAssetRequestDto("AB11");

            var violations = validator.validate(invalidPayload);

            assertEquals(1, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
        }
    }

    @Nested
    class AddPurchase {

        @Test
        @DisplayName("Should be able to add purchase in wallet created with valid payload")
        void shouldBeAbleToAddPurchaseInWalletCreatedWithValidPayload() {

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            String message = "A compra do seu ativo ABCD11 foi cadastrada com sucesso";

            when(walletService.addPurchaseToAsset(TOKEN, payload)).thenReturn(message);

            ResponseEntity<UpdateWalletResponseDto> response = walletController.addPurchase(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(walletService, times(1)).addPurchaseToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add purchase in wallet created when asset name does not exist")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWhenAssetNameDoesNotExist() {

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            String message = "O ativo informado não existe";

            when(walletService.addPurchaseToAsset(TOKEN, payload)).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.addPurchase(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addPurchaseToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add purchase when user wallet does not exist")
        void shouldNotBeAbleToAddPurchaseWhenUserWalletIsNotExist() {

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            String message = "Carteira não encontrada para o usuário informado";

            when(walletService.addPurchaseToAsset(TOKEN, payload)).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.addPurchase(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addPurchaseToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add purchase in wallet created when asset does not belong user wallet")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWhenAssetIsDoesNotUserBelongToUserWallet() {

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            String message = "O ativo informado não existe na carteira";

            when(walletService.addPurchaseToAsset(TOKEN, payload)).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.addPurchase(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addPurchaseToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add purchase in wallet created with empty asset name in payload")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWithEmptyAssetNameInPayload() {

            AddPurchaseRequestDto invalidPayload = new AddPurchaseRequestDto(
                    "",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to add purchase in wallet create with invalid payload")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWithInvalidPayload() {

            AddPurchaseRequestDto invalidPayload = new AddPurchaseRequestDto(
                    "AB11",
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(4, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O número de cotas compradas deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O preço da compra deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A data da compra não pode ser no futuro")));
        }

        private static @NotNull AddPurchaseRequestDto getAddPurchaseRequestDto() {
            return new AddPurchaseRequestDto(
                    "ABCD11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );
        }
    }

    @Nested
    class UpdatePurchase {

        @Test
        @DisplayName("Should be able to update purchase in wallet created with valid payload")
        void shouldBeAbleToUpdatePurchaseInWalletCreatedWithValidPayload() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            String message = "A compra purchaseId do ativo ABCD11 foi atualizada com sucesso";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            )).thenReturn(message);

            ResponseEntity<UpdateWalletResponseDto> response = walletController.updatePurchase(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase in wallet created with null payload")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWithEmptyPayload() {

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    null,
                    null,
                    null
            );

            String message = "Não há informações de compra para serem atualizadas";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload)
            ).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.updatePurchase(TOKEN,
                            "ABCD11",
                            "purchaseId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase in wallet created when asset name does not exist")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWhenAssetNameDoesNotExist() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            String message = "O ativo informado não existe";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updatePurchase(TOKEN,
                            "ABCD11",
                            "purchaseId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase when user wallet does not exist")
        void shouldNotBeAbleToUpdatePurchaseWhenUserWalletIsNotExist() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            String message = "Carteira não encontrada para o usuário informado";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updatePurchase(TOKEN,
                            "ABCD11",
                            "purchaseId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase in wallet created when asset does not belong to user wallet")
        void shouldNotBeAbleToUpdatePurchaseInWalletCreatedWhenAssetIsDoesNotBelongToUserWallet() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            String message = "O ativo informado não existe na carteira";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload)
            ).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.updatePurchase(TOKEN,
                            "ABCD11",
                            "purchaseId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase in wallet created when purchase does not belong to asset")
        void shouldNotBeAbleToUpdatePurchaseInWalletCreatedWhenPurchaseIsDoesNotBelongAsset() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            String message = "Não existe compra com o ID informado";

            when(walletService.updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updatePurchase(TOKEN,
                            "ABCD11",
                            "purchaseId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updatePurchaseToAssetByPurchaseId(
                    TOKEN,
                    "ABCD11",
                    "purchaseId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update purchase in wallet create with invalid payload")
        void shouldNotBeAbleToUpdatePurchaseInWalletCreatedWithInvalidPayload() {

            UpdatePurchaseRequestDto invalidPayload = new UpdatePurchaseRequestDto(
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O número de cotas compradas deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O preço da compra deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A data da compra não pode ser no futuro")));
        }

        private static @NotNull UpdatePurchaseRequestDto getUpdatePurchaseRequestDto() {
            return new UpdatePurchaseRequestDto(
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );
        }
    }

    @Nested
    class AddSale {

        @Test
        @DisplayName("Should be able to add sale in wallet created with valid payload")
        void shouldBeAbleToAddSaleInWalletCreatedWithValidPayload() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            String message = "A venda do seu ativo ABCD11 foi cadastrada com sucesso";

            when(walletService.addSaleToAsset(TOKEN, payload)).thenReturn(message);

            ResponseEntity<UpdateWalletResponseDto> response = walletController.addSale(TOKEN, payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale in wallet created when asset name does not exist")
        void shouldNotBeAbleToAddSaleInWalletCreatedWhenAssetNameDoesNotExist() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            String message = "O ativo informado não existe";

            when(walletService.addSaleToAsset(TOKEN, payload)).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.addSale(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale when user wallet does not exist")
        void shouldNotBeAbleToAddSaleWhenUserWalletIsNotExist() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            String message = "Carteira não encontrada para o usuário informado";

            when(walletService.addSaleToAsset(TOKEN, payload)).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.addSale(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale in wallet created when asset does not belong user wallet")
        void shouldNotBeAbleToAddSaleInWalletCreatedWhenAssetIsDoesNotUserBelongToUserWallet() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            String message = "O ativo informado não existe na carteira";

            when(walletService.addSaleToAsset(TOKEN, payload)).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.addSale(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale in wallet created when quota amount is less than sale amount")
        void shouldNotBeAbleToAddSaleInWalletCreatedWhenQuotaAmountIsLessThanSaleAmount() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            String message = "A quantidade de cota do ativo não pode ser negativa";

            when(walletService.addSaleToAsset(TOKEN, payload)).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.addSale(TOKEN, payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).addSaleToAsset(TOKEN, payload);
        }

        @Test
        @DisplayName("Should not be able to add sale in wallet created with empty asset name in payload")
        void shouldNotBeAbleToAddSaleInWalletCreatedWithEmptyAssetNameInPayload() {

            AddSaleRequestDto invalidPayload = new AddSaleRequestDto(
                    "",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to add sale in wallet create with invalid payload")
        void shouldNotBeAbleToAddSaleInWalletCreatedWithInvalidPayload() {

            AddSaleRequestDto invalidPayload = new AddSaleRequestDto(
                    "AB11",
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(4, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do ativo deve conter entre 5 e 6 caracteres")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O número de cotas vendidas deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O preço da venda deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A data da venda não pode ser no futuro")));
        }

        private static @NotNull AddSaleRequestDto getAddSaleRequestDto() {
            return new AddSaleRequestDto(
                    "ABCD11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );
        }
    }

    @Nested
    class UpdateSale {

        @Test
        @DisplayName("Should be able to update sale in wallet created with valid payload")
        void shouldBeAbleToUpdateSaleInWalletCreatedWithValidPayload() {

            UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

            String message = "A compra saleId do ativo ABCD11 foi atualizada com sucesso";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            )).thenReturn(message);

            ResponseEntity<UpdateWalletResponseDto> response = walletController.updateSale(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale in wallet created with null payload")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWithEmptyPayload() {

            UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                    null,
                    null,
                    null
            );

            String message = "Não há informações de compra para serem atualizadas";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload)
            ).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.updateSale(TOKEN,
                            "ABCD11",
                            "saleId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale in wallet created when asset name does not exist")
        void shouldNotBeAbleToAddPurchaseInWalletCreatedWhenAssetNameDoesNotExist() {

            UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

            String message = "O ativo informado não existe";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updateSale(TOKEN,
                            "ABCD11",
                            "saleId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale when user wallet does not exist")
        void shouldNotBeAbleToUpdateSaleWhenUserWalletIsNotExist() {

            UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

            String message = "Carteira não encontrada para o usuário informado";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updateSale(TOKEN,
                            "ABCD11",
                            "saleId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale in wallet created when asset does not belong to user wallet")
        void shouldNotBeAbleToUpdateSaleInWalletCreatedWhenAssetIsDoesNotBelongToUserWallet() {

            UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

            String message = "O ativo informado não existe na carteira";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload)
            ).thenThrow(
                    new BadRequestException(message)
            );

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> walletController.updateSale(TOKEN,
                            "ABCD11",
                            "saleId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale in wallet created when sale does not belong to asset")
        void shouldNotBeAbleToUpdateSaleInWalletCreatedWhenPurchaseIsDoesNotBelongAsset() {

            UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

            String message = "Não existe compra com o ID informado";

            when(walletService.updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload)
            ).thenThrow(
                    new ResourceNotFoundException(message)
            );

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> walletController.updateSale(TOKEN,
                            "ABCD11",
                            "saleId",
                            payload));

            assertEquals(message, exception.getMessage());
            verify(walletService, times(1)).updateSaleToAssetBySaleId(
                    TOKEN,
                    "ABCD11",
                    "saleId",
                    payload
            );
        }

        @Test
        @DisplayName("Should not be able to update sale in wallet create with invalid payload")
        void shouldNotBeAbleToUpdateSaleInWalletCreatedWithInvalidPayload() {

            UpdateSaleRequestDto invalidPayload = new UpdateSaleRequestDto(
                    0,
                    BigDecimal.valueOf(0),
                    Instant.now().plus(Duration.ofDays(1))
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O número de cotas vendidas deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O preço da venda deve ser maior que zero")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A data da venda não pode ser no futuro")));
        }

        private static @NotNull UpdateSaleRequestDto getUpdateSaleRequestDto() {
            return new UpdateSaleRequestDto(
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );
        }
    }
}
