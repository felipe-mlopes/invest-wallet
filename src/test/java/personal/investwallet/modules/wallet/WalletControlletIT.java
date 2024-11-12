package personal.investwallet.modules.wallet;

import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import personal.investwallet.modules.asset.AssetEntity;
import personal.investwallet.modules.asset.AssetRepository;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.modules.wallet.dto.*;
import personal.investwallet.security.TokenService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients.createDefault;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class WalletControlletIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl("testdb"));
        registry.add("security.token.secret", () -> "test-secret");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TokenService tokenService;

    @BeforeAll
    static void setUpContainer() {
        mongoDBContainer.start();
    }

    @AfterAll
    static void tearDownContainer() {
        mongoDBContainer.stop();
    }

    private String userId = null;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        assetRepository.deleteAll();
        walletRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setId(new ObjectId().toString());
        user.setName("Test User");
        user.setEmail("test.user@example.com");
        user.setPassword("password123");
        user.setChecked(true);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        AssetEntity asset = new AssetEntity();
        asset.setAssetName("ABCD11");
        asset.setAssetType("fundos-imboliarios");
        assetRepository.save(asset);

        String token = tokenService.generateToken(user);
        userId = user.getId();

        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add(HttpHeaders.COOKIE, "access_token=" + token);

        createDefault();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate.getRestTemplate().setRequestFactory(requestFactory);
    }

    @Nested
    @Order(1)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Create {

        @Test
        @Order(1)
        void createWallet_ShouldCreateNewWalletAndRegisterNewAssetSuccessfully() {

            CreateAssetRequestDto payload = getAssetCreateRequestDto();

            HttpEntity<CreateAssetRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Uma nova carteira foi criada e o ativo " + payload.assetName() + " foi adicionado";
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(walletRepository.findByUserId(userId).isPresent());
        }

        @Test
        @Order(2)
        void createWallet_ShouldRegisterNewAssetInExistingWalletSuccessfully() {

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            walletRepository.save(wallet);

            CreateAssetRequestDto payload = getAssetCreateRequestDto();

            HttpEntity<CreateAssetRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo " + payload.assetName() + " foi adicionado à carteira com sucesso";
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(walletRepository.findByUserId(userId).isPresent());
        }

        @Test
        @Order(3)
        void createWallet_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("XYZW11");

            HttpEntity<CreateAssetRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void createWallet_ShouldThrowConflictWhenAssetAlreadyBelongsToWallet() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            CreateAssetRequestDto payload = getAssetCreateRequestDto();

            HttpEntity<CreateAssetRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado já existe na carteira";
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        private static @NotNull CreateAssetRequestDto getAssetCreateRequestDto() {
            return new CreateAssetRequestDto(
                    "ABCD11"
            );
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AddPurchase {

        @Test
        @Order(1)
        void addPurchase_ShouldRegisterNewPurchaseOfAssetInWalletSuccessfully() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

             AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

             HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

             ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                     "/wallet/purchase",
                     requestEntity,
                     WallerSuccessResponseDto.class
             );

            String message = "A compra do seu ativo " + payload.assetName() + " foi cadastrada com sucesso" ;
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(walletRepository.findByUserId(userId).isPresent());
        }

        @Test
        @Order(2)
        void addPurchase_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            AddPurchaseRequestDto payload = new AddPurchaseRequestDto(
                    "XYZW11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );

            HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/purchase",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void addPurchase_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/purchase",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Carteira não encontrada para o usuário informado";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void addPurchase_ShouldThrowNotFoundWhenAssetDoesNotExistInWallet() {

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            walletRepository.save(wallet);

            AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

            HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/purchase",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe na carteira";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
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
    @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AddManyPurchasesByCSV {}

    @Nested
    @Order(4)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UpdatePurchase {

        @Test
        @Order(1)
        void updatePurchase_ShouldUpdatePurchaseByIdSuccessfully() {

            WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                    UUID.randomUUID().toString(),
                    100,
                    BigDecimal.valueOf(110.89),
                    BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(100)),
                    Instant.now().minus(Duration.ofDays(1))
            );

            WalletEntity.Asset asset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(List.of(purchase)),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", asset);
            walletRepository.save(wallet);

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + purchase.getPurchaseId(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "A compra " + purchase.getPurchaseId() + " do ativo " + asset.getAssetName() + " foi atualizada com sucesso";
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(2)
        void updatePurchase_ShouldThrowBadRequestOnEmptyPayload() {

            WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                    UUID.randomUUID().toString(),
                    10,
                    BigDecimal.valueOf(110.89),
                    BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(Duration.ofDays(1))
            );

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(List.of(purchase)),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                    null,
                    null,
                    null
            );

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Não há informações de compra para serem atualizadas";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void updatePurchase_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "XYZW11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void updatePurchase_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Carteira não encontrada para o usuário informado";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(5)
        void updatePurchase_ShouldThrowBadRequestWhenAssetDoesNotExistInWallet() {

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            walletRepository.save(wallet);

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe na carteira";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(6)
        void updatePurchase_ShouldThrowNotFoundWhenPurchaseIdDoesNotExistInPurchasesInfo() {

            WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                    UUID.randomUUID().toString(),
                    10,
                    BigDecimal.valueOf(110.89),
                    BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(Duration.ofDays(1))
            );

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(List.of(purchase)),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

            HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.PATCH,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Não existe compra com o ID informado";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        private static @NotNull UpdatePurchaseRequestDto getUpdatePurchaseRequestDto() {
            return new UpdatePurchaseRequestDto(
                    100,
                    BigDecimal.valueOf(54.78),
                    Instant.now().minus(Duration.ofDays(1))
            );
        }
    }

    @Nested
    @Order(5)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RemovePurchase {

        @Test
        @Order(1)
        void removePurchase_shouldDeletePurchaseByIdSuccessfully() {

            WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                    UUID.randomUUID().toString(),
                    10,
                    BigDecimal.valueOf(110.89),
                    BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(Duration.ofDays(1))
            );

            WalletEntity.Asset asset = new WalletEntity.Asset(
                    "ABCD11",
                    10,
                    new ArrayList<>(List.of(purchase)),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put(asset.getAssetName(), asset);
            walletRepository.save(wallet);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + asset.getAssetName() + "/purchases/" + purchase.getPurchaseId(),
                    HttpMethod.DELETE,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "A compra " + purchase.getPurchaseId() + " do ativo " + asset.getAssetName() + " foi removida com sucesso";

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(2)
        void removePurchase_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);;

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "XYZW11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.DELETE,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void removePurchase_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.DELETE,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Carteira não encontrada para o usuário informado";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void removePurchase_ShouldThrowBadRequestWhenAssetDoesNotExistInWallet() {

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            walletRepository.save(wallet);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.DELETE,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe na carteira";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(5)
        void removePurchase_ShouldThrowNotFoundWhenPurchaseIdDoesNotExistInPurchasesInfo() {

            WalletEntity.Asset.PurchasesInfo purchase = new WalletEntity.Asset.PurchasesInfo(
                    UUID.randomUUID().toString(),
                    10,
                    BigDecimal.valueOf(110.89),
                    BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                    Instant.now().minus(Duration.ofDays(1))
            );

            WalletEntity.Asset asset = new WalletEntity.Asset(
                    "ABCD11",
                    10,
                    new ArrayList<>(List.of(purchase)),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put(asset.getAssetName(), asset);
            walletRepository.save(wallet);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                    "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/" + UUID.randomUUID().toString(),
                    HttpMethod.DELETE,
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            System.out.println(response);

            String message = "Compra com o ID fornecido não encontrada";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }
    }

    @Nested
    @Order(6)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AddSale {

        @Test
        @Order(1)
        void addSale_ShouldRegisterNewSaleOfAssetInWalletSuccessfully() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    200,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            AddSaleRequestDto payload = getAddSaleRequestDto();

            HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/sale",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "A venda do seu ativo " + payload.assetName() + " foi cadastrada com sucesso" ;
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(walletRepository.findByUserId(userId).isPresent());
        }

        @Test
        @Order(2)
        void addSale_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    200,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            AddSaleRequestDto payload = new AddSaleRequestDto(
                    "XYZW11",
                    100,
                    BigDecimal.valueOf(50.00),
                    Instant.now().minus(Duration.ofDays(1))
            );

            HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/sale",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void addSale_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

            AddSaleRequestDto payload = getAddSaleRequestDto();

            HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/sale",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "Carteira não encontrada para o usuário informado";
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void addSale_ShouldThrowBadRequestWhenAssetDoesNotExistInWallet() {

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            walletRepository.save(wallet);

            AddSaleRequestDto payload = getAddSaleRequestDto();

            HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/sale",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "O ativo informado não existe na carteira";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(5)
        void addSale_ShouldThrowBadRequestWhenAssetAmountIsLessThanSaleAmount() {

            WalletEntity.Asset newAsset = new WalletEntity.Asset(
                    "ABCD11",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            WalletEntity wallet = new WalletEntity();
            wallet.setUserId(userId);
            wallet.getAssets().put("ABCD11", newAsset);
            walletRepository.save(wallet);

            AddSaleRequestDto payload = getAddSaleRequestDto();

            HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet/sale",
                    requestEntity,
                    WallerSuccessResponseDto.class
            );

            String message = "A quantidade de cota do ativo não pode ser negativa";
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(message, response.getBody().message());
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
}
