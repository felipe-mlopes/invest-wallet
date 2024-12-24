package personal.investwallet.modules.yield;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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
import personal.investwallet.modules.yield.dto.YieldAssetNameRequestDto;
import personal.investwallet.modules.yield.dto.YieldInfoByAssetNameResponseDto;
import personal.investwallet.modules.yield.dto.YieldInfoByYieldAtResponseDto;
import personal.investwallet.modules.yield.dto.YieldRequestDto;
import personal.investwallet.modules.yield.dto.YieldSuccessResponseDto;
import personal.investwallet.modules.yield.dto.YieldTimeIntervalRequestDto;
import personal.investwallet.security.TokenService;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients.createDefault;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Tag("integration")
public class YieldControllerIT {

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
        private YieldRepository yieldRepository;

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

        private final String ASSET_NAME = "ABCD11";
        private String userId = null;
        private HttpHeaders headers;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                assetRepository.deleteAll();
                yieldRepository.deleteAll();

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
        class CreateMany {

                @Test
                @Order(1)
                void createManyYields_ShouldRegisterNewYieldsSuccessfully() {

                        List<YieldRequestDto> payload = getAssetCreateRequestDto(ASSET_NAME);

                        HttpEntity<List<YieldRequestDto>> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/yield",
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Foram registrados " + payload.size() + " dividendos com sucesso.";

                        assertNotNull(response);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(yieldRepository.existsByUserAssetYieldAt(userId + ASSET_NAME + "202407"));
                }

                @Test
                @Order(2)
                void createManyYields_ShouldNotRegisterAnExistingYield() {

                        String userAssetYieldAt = userId + ASSET_NAME + "202407";

                        yieldRepository.save(new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        ASSET_NAME,
                                        "202407",
                                        userAssetYieldAt,
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1")));

                        List<YieldRequestDto> payload = getAssetCreateRequestDto(ASSET_NAME);

                        HttpEntity<List<YieldRequestDto>> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/yield",
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Foi registrado 1 dividendo com sucesso.";

                        assertNotNull(response);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(yieldRepository.existsByUserAssetYieldAt(userId + ASSET_NAME + "202408"));
                }

                @Test
                @Order(3)
                void createManyYields_ShouldThrowBadRequestWhenAllYieldsAreRegistered() {

                        yieldRepository.save(new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        ASSET_NAME,
                                        "202408",
                                        userId + ASSET_NAME + "202408",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1")));

                        YieldRequestDto yield = new YieldRequestDto(
                                        ASSET_NAME,
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        List<YieldRequestDto> payload = List.of(yield);

                        HttpEntity<List<YieldRequestDto>> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/yield",
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O(s) dividendo(s) enviado(s) já estão registrados.";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void createManyYields_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        List<YieldRequestDto> payload = getAssetCreateRequestDto("XYZW11");

                        HttpEntity<List<YieldRequestDto>> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.postForEntity(
                                        "/yield",
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull List<YieldRequestDto> getAssetCreateRequestDto(String assetName) {

                        YieldRequestDto yield1 = new YieldRequestDto(
                                        assetName,
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));
                        YieldRequestDto yield2 = new YieldRequestDto(
                                        assetName,
                                        Instant.parse("2024-07-31T00:00:00Z"),
                                        Instant.parse("2024-08-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        return List.of(yield1, yield2);
                }
        }

        @Nested
        @Order(2)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class CreateManyByCsv {

                @Test
                @Order(1)
                void createManyByCsv_ShouldRegisterAllYieldsSuccessfully() {

                        AssetEntity asset = new AssetEntity();
                        asset.setAssetName("XYZW11");
                        asset.setAssetType("fundos-imboliarios");
                        assetRepository.save(asset);

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O arquivo com 2 linhas foi registrado com sucesso.";
                        assertNotNull(response);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(yieldRepository.existsByUserAssetYieldAt(userId + ASSET_NAME + "202310"));
                }

                @Test
                @Order(2)
                void createManyByCsv_ShouldNotRegisterAnExistingYield() {

                        AssetEntity asset = new AssetEntity();
                        asset.setAssetName("XYZW11");
                        asset.setAssetType("fundos-imboliarios");
                        assetRepository.save(asset);

                        yieldRepository.save(new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        ASSET_NAME,
                                        "202310",
                                        userId + ASSET_NAME + "202310",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1")));

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O arquivo com 1 linha foi registrado com sucesso.";
                        assertNotNull(response);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                        assertTrue(yieldRepository.existsByUserAssetYieldAt(userId + "XYZW11" + "202310"));
                }

                @Test
                @Order(3)
                void createManyByCsv_ShouldThrowBadRequestWhenAllYieldsAreRegistered() {

                        AssetEntity asset = new AssetEntity();
                        asset.setAssetName("XYZW11");
                        asset.setAssetType("fundos-imboliarios");
                        assetRepository.save(asset);

                        YieldEntity yield1 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        ASSET_NAME,
                                        "202310",
                                        userId + ASSET_NAME + "202310",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        YieldEntity yield2 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        "XYZW11",
                                        "202310",
                                        userId + "XYZW11" + "202310",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        yieldRepository.saveAll(List.of(yield1, yield2));

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O(s) dividendo(s) enviado(s) já estão registrados.";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(4)
                void createManyByCsv_ShouldThrowBadRequestWithEmptyFile() {

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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O arquivo não enviado ou não preenchido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(5)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidFormatFile() {

                        String invalidCsvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O arquivo deve ser um CSV válido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(6)
                void createManyByCsv_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnQuantityInFile() {

                        String invalidHeaderContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value, Column Invalid
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Formato de cabeçalho inválido. Esperado: Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(7)
                void createManyByCsv_ShouldThrowBadRequestWhenHeaderContainsAnInvalidColumnNameInFile() {

                        String invalidHeaderContent = """
                                        Asset-name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Coluna inválida no cabeçalho. Esperado: 'Asset Name', Encontrado: 'Asset-name'";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(8)
                void createManyByCsv_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnNameInFile() {

                        String invalidRowContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                         , 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Na linha 3, a coluna 1 está vazia";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(9)
                void createManyByCsv_ShouldThrowBadRequestWhenRowContainsAnInvalidColumnQuantityInFile() {

                        String invalidRowContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05, Invalid
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "A linha 3 possui número incorreto de colunas";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(10)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidYieldAtFormatInFile() {

                        String invalidYieldAt = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310X, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidYieldAt.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O yieldAt deve conter apenas 6 caracteres contendo o ano (yyyy) e o mês (mm)";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(11)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidYearInYieldAtInFile() {

                        String invalidYieldAt = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 212010, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidYieldAt.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O ano informado no YieldAt deve ter 4 caracteres e ser menor ou igual ao ano corrente";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(12)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidMonthInYieldAtInFile() {

                        String invalidYieldAt = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202315, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidYieldAt.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O mês informado no YieldAt deve ter 2 caracteres e ser válido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(13)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidDateInFile() {

                        String invalidDate = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Erro na linha 2, Data Base: formato de data inválido. Use dd/MM/yyyy";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(14)
                void createManyByCsv_ShouldThrowBadRequestWhenBaseDateIsGreaterThanCurrentDateInFile() {

                        String invalidDate = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2120, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O ano da data base e/ou da data de pagamento precisa ser menor ou igual a ano corrente";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(15)
                void createManyByCsv_ShouldThrowBadRequestWhenBaseDateIsGreaterThanPaymentDateInFile() {

                        String invalidDate = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2024, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
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

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "A data de pagamento precisa ser maior que a data base de cálculo do dividendo";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(16)
                void createManyByCsv_ShouldThrowBadRequestWithInvalidPriceInFile() {

                        String invalidPrice = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 10X.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        MockMultipartFile csvFile = new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        invalidPrice.getBytes());

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "Erro na linha 2, Preço Base: valor numérico inválido";
                        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                @Test
                @Order(17)
                void createManyByCsv_ShouldThrowNotFoundWhenAssetNameDoesNotExist() {

                        MockMultipartFile csvFile = getMockMultipartFile();

                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", csvFile.getResource());

                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                        ResponseEntity<YieldSuccessResponseDto> response = restTemplate.exchange(
                                        "/yield/file",
                                        HttpMethod.POST,
                                        requestEntity,
                                        YieldSuccessResponseDto.class);

                        String message = "O ativo XYZW11 informado não existe";
                        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                        assertEquals(message, response.getBody().message());
                }

                private static @NotNull MockMultipartFile getMockMultipartFile() {

                        String csvContent = """
                                        Asset Name, Yield At, Base Date, Payment Date, Base Price, Income Value, Yield Value
                                        ABCD11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        XYZW11, 202310, 31/10/2023, 15/11/2023, 100.00, 5.00, 0.05
                                        """;

                        return new MockMultipartFile(
                                        "file",
                                        "file.csv",
                                        "text/csv",
                                        csvContent.getBytes());
                }
        }

        @Nested
        @Order(3)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class GetManyByUserIdAndYieldAt {

                @BeforeEach()
                void setUp() {
                        AssetEntity asset1 = new AssetEntity();
                        asset1.setAssetName("XYZW11");
                        asset1.setAssetType("fundos-imboliarios");

                        AssetEntity asset2 = new AssetEntity();
                        asset2.setAssetName("TEST11");
                        asset2.setAssetType("fundos-imboliarios");

                        assetRepository.save(asset1);
                        assetRepository.save(asset2);

                        List<YieldEntity> yieldList = getYieldEntities(userId);
                        yieldRepository.saveAll(yieldList);
                }

                @Test
                @Order(1)
                void getManyByUserIdAndYieldAt_ShouldReturnYieldsSuccessfully() {

                        YieldTimeIntervalRequestDto payload = new YieldTimeIntervalRequestDto(
                                        Instant.parse("2024-08-01T00:00:00Z"),
                                        Instant.parse("2024-08-31T00:00:00Z"));

                        HttpEntity<YieldTimeIntervalRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<Map<String, List<YieldInfoByYieldAtResponseDto>>> response = restTemplate
                                        .exchange(
                                                        "/yield/yield-at",
                                                        HttpMethod.GET,
                                                        requestEntity,
                                                        new ParameterizedTypeReference<>() {
                                                        });

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());

                        List<YieldInfoByYieldAtResponseDto> yields = response.getBody().get("202408");
                        assertEquals(2, yields.size());

                        YieldInfoByYieldAtResponseDto yieldInfo = yields.get(0);
                        assertEquals(ASSET_NAME, yieldInfo.assetName());
                }

                @Test
                @Order(2)
                void getManyByUserIdAndYieldAt_ShouldReturnEmptyMapWhenNoYieldsFound() {

                        YieldTimeIntervalRequestDto payload = new YieldTimeIntervalRequestDto(
                                        Instant.parse("2024-01-01T00:00:00Z"),
                                        Instant.parse("2024-03-31T00:00:00Z"));

                        HttpEntity<YieldTimeIntervalRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<Map<String, List<YieldInfoByYieldAtResponseDto>>> response = restTemplate
                                        .exchange(
                                                        "/yield/yield-at",
                                                        HttpMethod.GET,
                                                        requestEntity,
                                                        new ParameterizedTypeReference<>() {
                                                        });

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());

                        Map<String, List<YieldInfoByYieldAtResponseDto>> responseBody = response.getBody();
                        assertNotNull(responseBody);
                        assertTrue(responseBody.isEmpty());
                }

                private static @NotNull List<YieldEntity> getYieldEntities(String userId) {

                        YieldEntity yield1 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        "ABCD11",
                                        "202408",
                                        userId + "ABCD11" + "202408",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        YieldEntity yield2 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        "XYZW11",
                                        "202408",
                                        userId + "XYZW11" + "202408",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        YieldEntity yield3 = new YieldEntity(UUID.randomUUID().toString(),
                                        userId,
                                        "TEST11",
                                        "202409",
                                        userId + "TEST11" + "202409",
                                        Instant.parse("2024-09-30T00:00:00Z"),
                                        Instant.parse("2024-10-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        return List.of(yield1, yield2, yield3);
                }
        }

        @Nested
        @Order(4)
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class GetManyByUserIdAndAssetName {

                @BeforeEach()
                void setUp() {
                        AssetEntity asset1 = new AssetEntity();
                        asset1.setAssetName("XYZW11");
                        asset1.setAssetType("fundos-imboliarios");

                        AssetEntity asset2 = new AssetEntity();
                        asset2.setAssetName("TEST11");
                        asset2.setAssetType("fundos-imboliarios");

                        assetRepository.save(asset1);
                        assetRepository.save(asset2);

                        List<YieldEntity> yieldList = getYieldEntities(userId);
                        yieldRepository.saveAll(yieldList);
                }

                @Test
                @Order(1)
                void getManyByUserIdAndAssetName_ShouldReturnYieldsSuccessfully() {

                        YieldAssetNameRequestDto payload = new YieldAssetNameRequestDto(ASSET_NAME);

                        HttpEntity<YieldAssetNameRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<Map<String, List<YieldInfoByAssetNameResponseDto>>> response = restTemplate
                                        .exchange(
                                                        "/yield/asset-name",
                                                        HttpMethod.GET,
                                                        requestEntity,
                                                        new ParameterizedTypeReference<>() {
                                                        });

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());

                        List<YieldInfoByAssetNameResponseDto> yields = response.getBody().get(ASSET_NAME);
                        assertEquals(2, yields.size());

                        YieldInfoByAssetNameResponseDto yieldName = yields.get(0);
                        assertEquals("202408", yieldName.yieldAt());
                }

                @Test
                @Order(2)
                void getManyByUserIdAndAssetName_ShouldReturnEmptyMapWhenNoYieldsFound() {

                        YieldAssetNameRequestDto payload = new YieldAssetNameRequestDto("TEST11");

                        HttpEntity<YieldAssetNameRequestDto> requestEntity = new HttpEntity<>(payload, headers);

                        ResponseEntity<Map<String, List<YieldInfoByAssetNameResponseDto>>> response = restTemplate
                                        .exchange(
                                                        "/yield/asset-name",
                                                        HttpMethod.GET,
                                                        requestEntity,
                                                        new ParameterizedTypeReference<>() {
                                                        });

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());

                        assertNotNull(response);
                        assertEquals(HttpStatus.OK, response.getStatusCode());

                        Map<String, List<YieldInfoByAssetNameResponseDto>> responseBody = response.getBody();
                        assertNotNull(responseBody);
                        assertTrue(responseBody.isEmpty());
                }

                private static @NotNull List<YieldEntity> getYieldEntities(String userId) {

                        YieldEntity yield1 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        "ABCD11",
                                        "202408",
                                        userId + "ABCD11" + "202408",
                                        Instant.parse("2024-08-31T00:00:00Z"),
                                        Instant.parse("2024-09-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        YieldEntity yield2 = new YieldEntity(
                                        UUID.randomUUID().toString(),
                                        userId,
                                        "ABCD11",
                                        "202409",
                                        userId + "ABCD11" + "202409",
                                        Instant.parse("2024-09-30T00:00:00Z"),
                                        Instant.parse("2024-10-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        YieldEntity yield3 = new YieldEntity(UUID.randomUUID().toString(),
                                        userId,
                                        "XYZW11",
                                        "202409",
                                        userId + "XYZW11" + "202409",
                                        Instant.parse("2024-09-30T00:00:00Z"),
                                        Instant.parse("2024-10-15T00:00:00Z"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("0.1"));

                        return List.of(yield1, yield2, yield3);
                }
        }
}