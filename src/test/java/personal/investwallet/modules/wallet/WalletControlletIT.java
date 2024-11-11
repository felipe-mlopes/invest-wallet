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
import personal.investwallet.modules.wallet.dto.CreateAssetRequestDto;
import personal.investwallet.modules.wallet.dto.WallerSuccessResponseDto;
import personal.investwallet.security.TokenService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

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

    private String token = null;
    private String userId = null;

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

        token = tokenService.generateToken(user);
        userId = user.getId();

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

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add(HttpHeaders.COOKIE, "access_token=" + token);

            HttpEntity<CreateAssetRequestDto> request = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    request,
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

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add(HttpHeaders.COOKIE, "access_token=" + token);

            HttpEntity<CreateAssetRequestDto> request = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    request,
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
        void createWallet_ShouldThrowResourceNotFoundWhenAssetNameDoesNotExist() {

            CreateAssetRequestDto payload = new CreateAssetRequestDto("XYZW11");

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add(HttpHeaders.COOKIE, "access_token=" + token);

            HttpEntity<CreateAssetRequestDto> request = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    request,
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

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add(HttpHeaders.COOKIE, "access_token=" + token);

            HttpEntity<CreateAssetRequestDto> request = new HttpEntity<>(payload, headers);

            ResponseEntity<WallerSuccessResponseDto> response = restTemplate.postForEntity(
                    "/wallet",
                    request,
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
}
