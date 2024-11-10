package personal.investwallet.modules.user;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import personal.investwallet.exceptions.dto.RestGenericErrorResponseDto;
import personal.investwallet.modules.user.dto.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.ClassOrderer.OrderAnnotation;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestClassOrder(OrderAnnotation.class)
public class UserControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void setUpContainer() {
        mongoDBContainer.start();
    }

    @AfterAll
    static void tearDownContainer() {
        mongoDBContainer.stop();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        Cache cache = cacheManager.getCache("verificationCodes");
        if (cache != null) {
            cache.clear();
        }

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
        void createUser_shouldRegisterSuccessfully() {

            UserCreateRequestDto payload = getUserCreateRequestDto();

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/register",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "Usuário cadastrado com sucesso";
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(userRepository.findByEmail(payload.email()).isPresent());
        }

        @Test
        @Order(2)
        void createUser_shouldThrowConflictOnDuplicateEmail() {

            UserEntity user = new UserEntity();
            user.setEmail("john.doe@example.com");
            userRepository.save(user);

            UserCreateRequestDto payload = getUserCreateRequestDto();

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/register",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "Usuário já existe";
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        private static @NotNull UserCreateRequestDto getUserCreateRequestDto() {
            return new UserCreateRequestDto(
                    "John Doe",
                    "john.doe@example.com",
                    "Password123"
            );
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Validate {

        @Test
        @Order(1)
        void validateUser_shouldCheckSuccessfully() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            Cache cache = cacheManager.getCache("verificationCodes");
            if (cache != null) {
                cache.put("john.doe@example.com", "AB12");
            }

            UserValidateRequestDto payload = getUserValidateRequestDto();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserValidateRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.exchange(
                    "/user/validate",
                    HttpMethod.PATCH,
                    requestEntity,
                    UserSuccessResponseDto.class
            );

            String message = "Validação concluída com sucesso";

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertNull(cache.get("john.doe@example.com", String.class));
        }

        @Test
        @Order(2)
        void validateUser_shouldThrowUnauthorizedOnInvalidEmail() {

            UserValidateRequestDto payload = getUserValidateRequestDto();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserValidateRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.exchange(
                    "/user/validate",
                    HttpMethod.PATCH,
                    requestEntity,
                    UserSuccessResponseDto.class
            );

            String message = "Email inválido";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void validateUser_shouldThrowUnauthorizedOnInvalidCode() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserValidateRequestDto payload = getUserValidateRequestDto();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserValidateRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.exchange(
                    "/user/validate",
                    HttpMethod.PATCH,
                    requestEntity,
                    UserSuccessResponseDto.class
            );

            String message = "O código informado não confere";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(4)
        void validateUser_shouldThrowUnauthorizedOnExpiredCache() {

            CacheManager customCacheManager = new CacheManager() {
                @Override
                public Cache getCache(@NotNull String name) {
                    return null;
                }

                @Override
                public @NotNull Collection<String> getCacheNames() {
                    return Collections.emptyList();
                }
            };

            ReflectionTestUtils.setField(userService, "cacheManager", customCacheManager);

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserValidateRequestDto payload = getUserValidateRequestDto();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserValidateRequestDto> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.exchange(
                    "/user/validate",
                    HttpMethod.PATCH,
                    requestEntity,
                    UserSuccessResponseDto.class
            );

            String message = "Tempo de validação expirado";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        private static @NotNull UserValidateRequestDto getUserValidateRequestDto() {
            return new UserValidateRequestDto(
                    "john.doe@example.com",
                    "AB12"
            );
        }
    }

    @Nested
    @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Revalidate {

        @Test
        @Order(1)
        void revalidateUserCode_shouldResendCodeSuccessfully() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto("john.doe@example.com");

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/revalidate",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "Código de confirmação reenviado";
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(message, response.getBody().message());
            assertTrue(userRepository.findByEmail(payload.email()).isPresent());
        }

        @Test
        @Order(2)
        void revalidateUserCode_shouldThrowUnauthorizedOnInvalidEmail() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto("john.doe@example.com");

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/revalidate",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "Email inválido";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Test
        @Order(3)
        void revalidateUserCode_shouldThrowConflictOnAlreadyChecked() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            user.setChecked(true);
            userRepository.save(user);

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto("john.doe@example.com");

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/revalidate",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "O cadastro do usuário já está válido";
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }

        @Disabled
        @Test
        @Order(4)
        void revalidateUserCode_shouldThrowConflictWhenVerificationCodeIsStillValid() {

            CacheManager customCacheManager = new CacheManager() {
                @Override
                public Cache getCache(@NotNull String name) {
                    return null;
                }

                @Override
                public @NotNull Collection<String> getCacheNames() {
                    return Collections.emptyList();
                }
            };

            ReflectionTestUtils.setField(userService, "cacheManager", customCacheManager);

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword("Password123");
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            Cache cache = cacheManager.getCache("verificationCodes");
            cache.put("john.doe@example.com", "AB12");
            String cachedCode = cache.get("john.doe@example.com", String.class);
            assertNotNull(cachedCode);
            assertEquals("AB12", cachedCode);

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto("john.doe@example.com");

            ResponseEntity<UserSuccessResponseDto> response = restTemplate.postForEntity(
                    "/user/revalidate",
                    payload,
                    UserSuccessResponseDto.class
            );

            String message = "O código de verificação enviado anteriormente ainda está válido";
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(message, response.getBody().message());
        }
    }

    @Nested
    @Order(4)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Login {

        @Test
        @Order(1)
        void loginUser_shouldGetAccessSuccessfully() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword(passwordEncoder.encode("Password123"));
            user.setChecked(true);
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserLoginRequestDto payload = getUserLoginRequestDto();

            ResponseEntity<TokenResponseDto> response = restTemplate.postForEntity(
                    "/user/login",
                    payload,
                    TokenResponseDto.class
            );

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody().token());
            assertTrue(userRepository.findByEmail(payload.email()).isPresent());
        }

        @Test
        @Order(2)
        void loginUser_shouldThrowUnauthorizedOnInvalidEmail() {

            UserLoginRequestDto payload = getUserLoginRequestDto();

            ResponseEntity<RestGenericErrorResponseDto> response = restTemplate.postForEntity(
                    "/user/login",
                    payload,
                    RestGenericErrorResponseDto.class
            );

            String message = "Usuário e/ou senha inválidos";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().getMessage());
        }

        @Test
        @Order(3)
        void loginUser_shouldThrowUnauthorizedOnInvalidPassword() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword(passwordEncoder.encode("password456"));
            user.setChecked(true);
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserLoginRequestDto payload = getUserLoginRequestDto();

            ResponseEntity<RestGenericErrorResponseDto> response = restTemplate.postForEntity(
                    "/user/login",
                    payload,
                    RestGenericErrorResponseDto.class
            );

            String message = "Usuário e/ou senha inválidos";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().getMessage());
        }

        @Test
        @Order(4)
        void loginUser_shouldThrowUnauthorizedOnUnverifiedUser() {

            UserEntity user = new UserEntity();
            user.setName("John Doe");
            user.setEmail("john.doe@example.com");
            user.setPassword(passwordEncoder.encode("Password123"));
            user.setCreatedAt(Instant.now());
            userRepository.save(user);

            UserLoginRequestDto payload = getUserLoginRequestDto();

            ResponseEntity<RestGenericErrorResponseDto> response = restTemplate.postForEntity(
                    "/user/login",
                    payload,
                    RestGenericErrorResponseDto.class
            );

            String message = "Usuário não confirmou seu cadastro por e-mail";
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(message, response.getBody().getMessage());
        }

        private static @NotNull UserLoginRequestDto getUserLoginRequestDto() {
            return new UserLoginRequestDto(
                    "john.doe@example.com",
                    "Password123"
            );
        }
    }

}
