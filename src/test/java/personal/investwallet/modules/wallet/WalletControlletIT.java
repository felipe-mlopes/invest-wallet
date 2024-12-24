package personal.investwallet.modules.wallet;

import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
@Tag("integration")
public class WalletControlletIT {

        @SuppressWarnings("resource")
        @Container
        static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
                        .withExposedPorts(27018)
                        .withEnv("MONGODB_PORT", "27018")
                        .withStartupTimeout(Duration.ofSeconds(120))
                        .withReuse(true)
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
                                        WallerSuccessResponseDto.class);

                        String message = "Uma nova carteira foi criada e o ativo " + payload.assetName()
                                        + " foi adicionado";
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
                                        WallerSuccessResponseDto.class);

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
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo " + payload.assetName() + " informado não existe";
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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        CreateAssetRequestDto payload = getAssetCreateRequestDto();

                        HttpEntity<CreateAssetRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo informado já existe na carteira";
                        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull CreateAssetRequestDto getAssetCreateRequestDto() {
                        return new CreateAssetRequestDto(
                                        "ABCD11");
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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        AddPurchaseRequestDto payload = getAddPurchaseRequestDto();

                        HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet/purchase",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A compra do seu ativo " + payload.assetName() + " foi cadastrada com sucesso";
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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        AddPurchaseRequestDto payload = new AddPurchaseRequestDto(
                                        "XYZW11",
                                        100,
                                        BigDecimal.valueOf(50.00),
                                        Instant.now().minus(Duration.ofDays(1)));

                        HttpEntity<AddPurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet/purchase",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo " + payload.assetName() + " informado não existe";
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
                                        WallerSuccessResponseDto.class);

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
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo informado não existe na carteira";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull AddPurchaseRequestDto getAddPurchaseRequestDto() {
                        return new AddPurchaseRequestDto(
                                        "ABCD11",
                                        100,
                                        BigDecimal.valueOf(50.00),
                                        Instant.now().minus(Duration.ofDays(1)));
                }
        }

        @Nested
        @Order(3)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class AddManyPurchasesByCSV {

                @Test
                @Order(1)
                void addManyPurchasesByCSV_ShouldRegisterNewWalletWithAssetAndAddManyPurchasesByFileSuccessfully() {

                        AssetEntity newAsset = new AssetEntity();
                        newAsset.setAssetName("XYZW11");
                        newAsset.setAssetType("fundos-imboliarios");

                        assetRepository.save(newAsset);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Uma carteira foi criada e os registros de compras foram cadastrados com sucesso";
                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(walletRepository.findByUserId(userId).isPresent());
                }

                @Test
                @Order(2)
                void addManyPurchasesByCSV_ShouldAddManyPurchasesOfAssetByFileInWalletSuccessfully() {

                        AssetEntity newAsset = new AssetEntity();
                        newAsset.setAssetName("XYZW11");
                        newAsset.setAssetType("fundos-imboliarios");
                        assetRepository.save(newAsset);

                        WalletEntity.Asset assetA = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity.Asset assetB = new WalletEntity.Asset(
                                        "XYZW11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", assetA);
                        wallet.getAssets().put("XYZW11", assetB);
                        walletRepository.save(wallet);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Os registros de compras foram cadastrados na carteira com sucesso";
                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(walletRepository.findByUserId(userId).isPresent());
                }

                @Test
                @Order(3)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithEmptyFile() {

                        String invalidCsvContent = """
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidCsvContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O arquivo não enviado ou não preenchido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithInvalidFormatFile() {

                        String invalidCsvContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,28.51
                                        XYZW11,02/10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.txt",
                                        "text/plain",
                                        invalidCsvContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O arquivo deve ser um CSV válido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(5)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnNameInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Invalid Column
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Coluna inválida no cabeçalho. Esperado: 'Value / Quota', Encontrado: 'Invalid Column'";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(6)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnQuantityInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058,INVALID
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A linha 3 possui número incorreto de colunas";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(7)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnNameInFile() {

                        String invalidRowContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                         ,02/10/2024,5,120.29,10.85
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidRowContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Na linha 3, a coluna 1 está vazia";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(8)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnQuantityInFile() {

                        String invalidRowContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058, Invalid
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidRowContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A linha 3 possui número incorreto de colunas";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(9)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithInvalidDateInFile() {

                        String invalidDate = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidDate.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Data: formato de data inválido. Use dd/MM/yyyy";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(10)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWhenDateEnteredIsGreaterThanCurrentDateInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2035,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A data informada precisa ser menor ou igual a data corrente";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(11)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithInvalidAmountInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5x,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Quantidade: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(12)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithInvalidPriceInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,12X.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Preço: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(13)
                void addManyPurchasesByCSV_ShouldThrowBadRequestWithInvalidQuotaValueInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,2Y.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Valor da cota: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(14)
                void addManyPurchasesByCSV_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/purchases",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }
        }

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
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(List.of(purchase)),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", asset);
                        walletRepository.save(wallet);

                        UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

                        HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + purchase.getPurchaseId(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A compra " + purchase.getPurchaseId() + " do ativo " + asset.getAssetName()
                                        + " foi atualizada com sucesso";
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
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(List.of(purchase)),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdatePurchaseRequestDto payload = new UpdatePurchaseRequestDto(
                                        null,
                                        null,
                                        null);

                        HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

                        HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "XYZW11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void updatePurchase_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

                        UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

                        HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(List.of(purchase)),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdatePurchaseRequestDto payload = getUpdatePurchaseRequestDto();

                        HttpEntity<UpdatePurchaseRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Não existe compra com o ID informado";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull UpdatePurchaseRequestDto getUpdatePurchaseRequestDto() {
                        return new UpdatePurchaseRequestDto(
                                        100,
                                        BigDecimal.valueOf(54.78),
                                        Instant.now().minus(Duration.ofDays(1)));
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
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        10,
                                        new ArrayList<>(List.of(purchase)),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put(asset.getAssetName(), asset);
                        walletRepository.save(wallet);

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + asset.getAssetName() + "/purchases/"
                                                        + purchase.getPurchaseId(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A compra " + purchase.getPurchaseId() + " do ativo " + asset.getAssetName()
                                        + " foi removida com sucesso";

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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);
                        ;

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "XYZW11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(3)
                void removePurchase_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        10,
                                        new ArrayList<>(List.of(purchase)),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put(asset.getAssetName(), asset);
                        walletRepository.save(wallet);

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/purchases/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        AddSaleRequestDto payload = getAddSaleRequestDto();

                        HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet/sale",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A venda do seu ativo " + payload.assetName() + " foi cadastrada com sucesso";
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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        AddSaleRequestDto payload = new AddSaleRequestDto(
                                        "XYZW11",
                                        100,
                                        BigDecimal.valueOf(50.00),
                                        Instant.now().minus(Duration.ofDays(1)));

                        HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet/sale",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo " + payload.assetName() + " informado não existe";
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
                                        WallerSuccessResponseDto.class);

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
                                        WallerSuccessResponseDto.class);

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
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        AddSaleRequestDto payload = getAddSaleRequestDto();

                        HttpEntity<AddSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/wallet/sale",
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A quantidade de cota do ativo não pode ser negativa";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull AddSaleRequestDto getAddSaleRequestDto() {
                        return new AddSaleRequestDto(
                                        "ABCD11",
                                        100,
                                        BigDecimal.valueOf(50.00),
                                        Instant.now().minus(Duration.ofDays(1)));
                }

        }

        @Nested
        @Order(7)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class AddManySalesByCSV {

                @Test
                @Order(1)
                void addManySalesByCSV_ShouldAddManySalesOfAssetByFileInWalletSuccessfully() {

                        AssetEntity newAsset = new AssetEntity();
                        newAsset.setAssetName("XYZW11");
                        newAsset.setAssetType("fundos-imboliarios");
                        assetRepository.save(newAsset);

                        WalletEntity.Asset assetA = new WalletEntity.Asset(
                                        "ABCD11",
                                        100,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity.Asset assetB = new WalletEntity.Asset(
                                        "XYZW11",
                                        100,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", assetA);
                        wallet.getAssets().put("XYZW11", assetB);
                        walletRepository.save(wallet);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Os registros de vendas foram cadastrados na carteira com sucesso";
                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(walletRepository.findByUserId(userId).isPresent());
                }

                @Test
                @Order(2)
                void addManySalesByCSV_ShouldThrowBadRequestWhenWalletHasNotBeenCreated() {

                        AssetEntity newAsset = new AssetEntity();
                        newAsset.setAssetName("XYZW11");
                        newAsset.setAssetType("fundos-imboliarios");

                        assetRepository.save(newAsset);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Não é possível adicionar um venda a uma nova carteira antes de inserir uma compra";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(3)
                void addManySalesByCSV_ShouldThrowBadRequestWithEmptyFile() {

                        String invalidCsvContent = """
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidCsvContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O arquivo não enviado ou não preenchido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void addManySalesByCSV_ShouldThrowBadRequestWithInvalidFormatFile() {

                        String invalidCsvContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,28.51
                                        XYZW11,02/10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.txt",
                                        "text/plain",
                                        invalidCsvContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O arquivo deve ser um CSV válido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(5)
                void addManySalesByCSV_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnQuantityInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058,INVALID
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A linha 3 possui número incorreto de colunas";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(6)
                void addManySalesByCSV_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnNameInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Invalid Column
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Coluna inválida no cabeçalho. Esperado: 'Value / Quota', Encontrado: 'Invalid Column'";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(7)
                void addManySalesByCSV_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnNameInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                         ,02/10/2024,5,120.29,10.85
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Na linha 3, a coluna 1 está vazia";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(8)
                void addManySalesByCSV_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnQuantityInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Formato de cabeçalho inválido. Esperado: Asset Name, Date, Amount, Quota Price, Value / Quota";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(9)
                void addManySalesByCSV_ShouldThrowBadRequestWithInvalidDateInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,10/2024,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Data: formato de data inválido. Use dd/MM/yyyy";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(10)
                void addManySalesByCSV_ShouldThrowBadRequestWhenDateEnteredIsGreaterThanCurrentDateInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2035,5,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A data informada precisa ser menor ou igual a data corrente";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(11)
                void addManySalesByCSV_ShouldThrowBadRequestWithInvalidAmountInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5x,120.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Quantidade: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(12)
                void addManySalesByCSV_ShouldThrowBadRequestWithInvalidPriceInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,12X.29,24.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Preço: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(13)
                void addManySalesByCSV_ShouldThrowBadRequestWithInvalidQuotaValueInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Date, Amount, Quota Price, Value / Quota
                                        ABCD11,01/01/2024,10,28.51,2.851
                                        ABCD11,02/10/2024,5,120.29,2Y.058
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidHeaderContent.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Erro na linha 3, Valor da cota: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(14)
                void addManySalesByCSV_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(15)
                void addManySalesByCSV_ShouldThrowBadRequestWhenSaleAmountIsGreaterThanAssetQuotaAmount() {

                        AssetEntity newAsset = new AssetEntity();
                        newAsset.setAssetName("XYZW11");
                        newAsset.setAssetType("fundos-imboliarios");
                        assetRepository.save(newAsset);

                        WalletEntity.Asset assetA = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity.Asset assetB = new WalletEntity.Asset(
                                        "XYZW11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", assetA);
                        wallet.getAssets().put("XYZW11", assetB);
                        walletRepository.save(wallet);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/sales",
                                        HttpMethod.POST,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A quantidade de cotas do ativo não pode ser negativa";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }
        }

        @Nested
        @Order(8)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class UpdateSale {

                @Test
                @Order(1)
                void updateSale_ShouldUpdateSaleByIdSuccessfully() {

                        WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                                        UUID.randomUUID().toString(),
                                        100,
                                        BigDecimal.valueOf(110.89),
                                        BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(100)),
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>(List.of(sale)));

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", asset);
                        walletRepository.save(wallet);

                        UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/" + sale.getSaleId(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A venda " + sale.getSaleId() + " do ativo " + asset.getAssetName()
                                        + " foi atualizada com sucesso";
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(2)
                void updateSale_ShouldThrowBadRequestOnEmptyPayload() {

                        WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                                        UUID.randomUUID().toString(),
                                        10,
                                        BigDecimal.valueOf(110.89),
                                        BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>(List.of(sale)));

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdateSaleRequestDto payload = new UpdateSaleRequestDto(
                                        null,
                                        null,
                                        null);

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Não há informações de venda para serem atualizadas";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(3)
                void updateSale_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "XYZW11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void updateSale_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

                        UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Carteira não encontrada para o usuário informado";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(5)
                void updateSale_ShouldThrowBadRequestWhenAssetDoesNotExistInWallet() {

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        walletRepository.save(wallet);

                        UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo informado não existe na carteira";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(6)
                void updateSale_ShouldThrowNotFoundWhenSaleIdDoesNotExistInSalesInfo() {

                        WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                                        UUID.randomUUID().toString(),
                                        10,
                                        BigDecimal.valueOf(110.89),
                                        BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>(List.of(sale)));

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);

                        UpdateSaleRequestDto payload = getUpdateSaleRequestDto();

                        HttpEntity<UpdateSaleRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.PATCH,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Não existe venda com o ID informado";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull UpdateSaleRequestDto getUpdateSaleRequestDto() {
                        return new UpdateSaleRequestDto(
                                        100,
                                        BigDecimal.valueOf(54.78),
                                        Instant.now().minus(Duration.ofDays(1)));
                }

        }

        @Nested
        @Order(9)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class RemoveSale {

                @Test
                @Order(1)
                void removeSale_shouldDeleteSaleByIdSuccessfully() {

                        WalletEntity.Asset.SalesInfo sale = new WalletEntity.Asset.SalesInfo(
                                        UUID.randomUUID().toString(),
                                        10,
                                        BigDecimal.valueOf(110.89),
                                        BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        10,
                                        new ArrayList<>(),
                                        new ArrayList<>(List.of(sale)));

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put(asset.getAssetName(), asset);
                        walletRepository.save(wallet);

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + asset.getAssetName() + "/sales/"
                                                        + sale.getSaleId(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "A venda " + sale.getSaleId() + " do ativo " + asset.getAssetName()
                                        + " foi removida com sucesso";

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(2)
                void removeSale_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        WalletEntity.Asset newAsset = new WalletEntity.Asset(
                                        "ABCD11",
                                        0,
                                        new ArrayList<>(),
                                        new ArrayList<>());

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put("ABCD11", newAsset);
                        walletRepository.save(wallet);
                        ;

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "XYZW11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(3)
                void removeSale_ShouldThrowNotFoundWhenNoWalletExistsForUserEntered() {

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Carteira não encontrada para o usuário informado";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void removeSale_ShouldThrowBadRequestWhenAssetDoesNotExistInWallet() {

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        walletRepository.save(wallet);

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "O ativo informado não existe na carteira";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(5)
                void removeSale_ShouldThrowNotFoundWhenSaleIdDoesNotExistInSalesInfo() {

                        WalletEntity.Asset.SalesInfo purchase = new WalletEntity.Asset.SalesInfo(
                                        UUID.randomUUID().toString(),
                                        10,
                                        BigDecimal.valueOf(110.89),
                                        BigDecimal.valueOf(110.89).divideToIntegralValue(BigDecimal.valueOf(10)),
                                        Instant.now().minus(Duration.ofDays(1)));

                        WalletEntity.Asset asset = new WalletEntity.Asset(
                                        "ABCD11",
                                        10,
                                        new ArrayList<>(),
                                        new ArrayList<>(List.of(purchase)));

                        WalletEntity wallet = new WalletEntity();
                        wallet.setUserId(userId);
                        wallet.getAssets().put(asset.getAssetName(), asset);
                        walletRepository.save(wallet);

                        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<WallerSuccessResponseDto> response = restTemplate.exchange(
                                        "/wallet/fundos-imobiliarios/" + "ABCD11" + "/sales/"
                                                        + UUID.randomUUID().toString(),
                                        HttpMethod.DELETE,
                                        requestEntity,
                                        WallerSuccessResponseDto.class);

                        String message = "Venda com o ID fornecido não encontrada";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

        }

        private static @NotNull MockMultipartFile getMockMultipartFile() {

                String csvContent = """
                                Asset Name, Date, Amount, Quota Price, Value / Quota
                                ABCD11,01/01/2024,10,28.51,28.51
                                XYZW11,02/10/2024,5,120.29,24.058
                                """;

                return new MockMultipartFile(
                                "file",
                                "file.csv",
                                "text/csv",
                                csvContent.getBytes());
        }
}
