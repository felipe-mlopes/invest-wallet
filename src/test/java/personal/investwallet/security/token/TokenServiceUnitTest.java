package personal.investwallet.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import personal.investwallet.exceptions.BadRequestException;
import personal.investwallet.exceptions.JWTGenerateFailedException;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.security.TokenService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenServiceUnitTest {

    @Mock
    private UserEntity userEntity;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(tokenService, "secret", "mySecretKey");
    }

    @Nested
    class GenerateToken {

        @Test
        @DisplayName("Should be able to generate token")
        void shouldBeAbleToGenerateToken() {

            when(userEntity.getId()).thenReturn("1234");

            String result = tokenService.generateToken(userEntity);

            assertNotNull(result);
            verify(userEntity, times(1)).getId();
        }

        @Test
        @DisplayName("Should not be able to generate token when secret is null")
        void shouldNotBeAbleToGenerateTokenWhenSecretIsNull() {

            ReflectionTestUtils.setField(tokenService, "secret", null);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> tokenService.generateToken(userEntity));

            assertEquals("A chave secreta não pode ser nula", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to generate token when JWT creation fails")
        void shouldNotBeAbleToGenerateTokenWhenJWTCreationFails() {

            Instant expiresAt = LocalDateTime
                    .now()
                    .plusHours(2)
                    .toInstant(ZoneOffset.of("-03:00"));

            Algorithm algorithm = Algorithm.HMAC256("mySecretKey");

            try (MockedStatic<JWT> mockedJWT = mockStatic(JWT.class)) {

                mockedJWT.when(() ->
                        JWT
                                .create()
                                .withIssuer("login-auth-api")
                                .withSubject(userEntity.getId())
                                .withExpiresAt(expiresAt)
                                .sign(algorithm))
                        .thenThrow(new JWTGenerateFailedException("Erro inesperado enquanto estava autenticando"));

                JWTGenerateFailedException exception = assertThrows(JWTGenerateFailedException.class,
                        () -> tokenService.generateToken(userEntity));

                assertEquals("Erro inesperado enquanto estava autenticando", exception.getMessage());
            }
        }
    }

    @Nested
    class GenerateExpirationDate {

        @Test
        @DisplayName("Should be able to generate expiration date")
        void shouldBeAbleToGenerateExpirationDate() {

            Instant expirationDate = ReflectionTestUtils.invokeMethod(tokenService, "generateExpirationDate");

            assertNotNull(expirationDate);
            assertTrue(expirationDate.isAfter(Instant.now()));
        }
    }

    @Nested
    class ExtractUserIdFromToken {

        @Test
        @DisplayName("Should be able to extract user id from token")
        void shouldBeAbleToExtractUserIdFromToken() {

            String validToken = JWT.create()
                    .withIssuer("login-auth-api")
                    .withSubject("user123")
                    .withExpiresAt(Instant.now().plusSeconds(3600))
                    .sign(Algorithm.HMAC256("mySecretKey"));

            String result = tokenService.extractUserIdFromToken(validToken);

            assertEquals("user123", result);
        }
    }

    @Nested
    class AddTokenToCookies {

        @Test
        @DisplayName("Should be able to add token to cookies")
        void shouldBeAbleToAddTokenToCookies() {

            String token = "test-token";

            tokenService.addTokenToCookies(token, httpServletResponse);

            verify(httpServletResponse, times(1)).addCookie(argThat(cookie ->
                    "access_token".equals(cookie.getName()) &&
                    token.equals(cookie.getValue()) &&
                    cookie.getMaxAge() == 24 * 60 * 60 &&
                    "/".equals(cookie.getPath())
            ));
        }
    }
}
