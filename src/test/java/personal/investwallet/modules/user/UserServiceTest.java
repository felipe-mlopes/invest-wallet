package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.UnauthorizedException;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.modules.user.dto.UserRevalidateRequestDto;
import personal.investwallet.modules.user.dto.UserValidateRequestDto;
import personal.investwallet.security.TokenService;

import java.time.Instant;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserService userService;

    @Nested
    class createUser {

        @Test
        @DisplayName("Should be able to create a new user")
        void shouldBeAbleToCreateNewUser() {

            UserCreateRequestDto payload = getUserCreateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(payload.password())).thenReturn("encodedPassword123");

            String result = userService.createUser(payload);

            verify(userRepository, times(1)).insert(any(UserEntity.class));
            assertEquals("Usuário cadastrado com sucesso.", result);

        }

        @Test
        @DisplayName("Should not be able to create a new user if email already exists")
        void shouldNotBeAbleToCreateNewUserIfEmailAlreadyExists() {

            UserCreateRequestDto payload = getUserCreateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(new UserEntity()));

            verify(userRepository, never()).insert(any(UserEntity.class));
            ConflictException exception = assertThrows(ConflictException.class, () -> userService.createUser(payload));
            assertEquals("Usuário já existe.", exception.getMessage());
        }
    }

    @Nested
    class validateUser {

        @Test
        @DisplayName("Should be able to validate user registration")
        void shouldBeAbleToValidateUserRegistration() {

            UserValidateRequestDto payload = getUserValidateRequestDto();

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail("john.doe@example.com");

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(cacheManager.getCache("verificationCodes")).thenReturn(cache);
            when(cache.get(payload.email(), String.class)).thenReturn("A1B2");

            String result = userService.validateUser(payload);

            verify(userRepository, times(1)).updateCheckedAsTrueByEmail(
                    eq(payload.email()),
                    any(Instant.class)
            );
            verify(cache).evict(payload.email());
            assertEquals("Validação concluída com sucesso!", result);
        }

        @Test
        @DisplayName("Should not be able to validate user registration with an invalid email")
        void shouldNotBeAbleToValidateUserRegistrationWithAnInvalidEmail() {

            UserValidateRequestDto payload = getUserValidateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.empty());

            verify(userRepository, never()).updateCheckedAsTrueByEmail(anyString(), any(Instant.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.validateUser(payload));
            assertEquals("Email inválido.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to validate user registration with an invalid verification code")
        void shouldNotBeAbleToValidateUserRegistrationWithAnInvalidVerificationCode() {

            UserValidateRequestDto payload = getUserValidateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(new UserEntity()));
            when(cacheManager.getCache("verificationCodes")).thenReturn(cache);
            when(cache.get(payload.email(), String.class)).thenReturn("1234");

            verify(userRepository, never()).updateCheckedAsTrueByEmail(anyString(), any(Instant.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.validateUser(payload));
            assertEquals("O código informado não confere.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to validate user registration with an expired verification code")
        void shouldNotBeAbleToValidateUserRegistrationWithAnExpiredVerificationCode() {

            UserValidateRequestDto payload = getUserValidateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(new UserEntity()));
            when(cacheManager.getCache("verificationCodes")).thenReturn(null);

            verify(userRepository, never()).updateCheckedAsTrueByEmail(anyString(), any(Instant.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.validateUser(payload));
            assertEquals("Tempo de validação expirado.", exception.getMessage());
        }
    }

    @Nested
    class verifyExistingUserAndVerificationCode {

        @Test
        @DisplayName("Should be able to verify existing user and verification code")
        void shouldBeAbleToVerifyExistingUserAndVerificationCode() {

            UserRevalidateRequestDto payload = getUserRevalidateRequestDto();

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(payload.email());

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(cacheManager.getCache("verificationCodes")).thenReturn(cache);
            when(cache.get(payload.email(), String.class)).thenReturn(null);

            assertDoesNotThrow(() -> userService.verifyExistingUserAndVerificationCode(payload));
        }

        @Test
        @DisplayName("Should not be able to verify existing user and verification code with an invalid email")
        void shouldNotBeAbleToVerifyExistingUserAndVerificationCodeWithAnInvalidEmail() {

            UserRevalidateRequestDto payload = getUserRevalidateRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.empty());

            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.verifyExistingUserAndVerificationCode(payload));
            assertEquals("Email inválido.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to verify existing user and verification code with user registration already checked")
        void shouldNotBeAbleToVerifyExistingUserAndVerificationCodeWithUserRegistrationAlreadyChecked() {

            UserRevalidateRequestDto payload = getUserRevalidateRequestDto();

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(payload.email());
            userEntity.setChecked(true);

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));

            ConflictException exception = assertThrows(ConflictException.class, () -> userService.verifyExistingUserAndVerificationCode(payload));
            assertEquals("O cadastro do usuário já está válido.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to verify existing user and verification code with a code that is still valid")
        void shouldNotBeAbleToVerifyExistingUserAndVerificationCodeWithACodeThatIsStillValid() {

            UserRevalidateRequestDto payload = getUserRevalidateRequestDto();

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(payload.email());

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(cacheManager.getCache("verificationCodes")).thenReturn(cache);
            when(cache.get(payload.email(), String.class)).thenReturn("ABCD");

            ConflictException exception = assertThrows(ConflictException.class, () -> userService.verifyExistingUserAndVerificationCode(payload));
            assertEquals("O código de verificação enviado anteriormente ainda está válido.", exception.getMessage());
        }
    }

    @Nested
    class authUser {

        @Test
        @DisplayName("Should be able to login")
        void shouldBeAbleToLogin() {

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail("john.doe@example.com");
            userEntity.setPassword("encodedPassword123");
            userEntity.setChecked(true);

            UserLoginRequestDto payload = getUserLoginRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(payload.password(),userEntity.getPassword())).thenReturn(true);
            when(tokenService.generateToken(userEntity)).thenReturn("generatedToken123");

            String token = userService.authUser(payload, response);

            verify(userRepository, times(1)).findByEmail(payload.email());
            verify(passwordEncoder, times(1)).matches(payload.password(), userEntity.getPassword());
            verify(tokenService, times(1)).generateToken(userEntity);
            verify(tokenService, times(1)).addTokenToCookies("generatedToken123", response);
            assertEquals("generatedToken123", token);
        }

        @Test
        @DisplayName("Should not be able to login if invalid email")
        void shouldNotBeAbleToLoginIfInvalidEmail() {

            UserLoginRequestDto payload = new UserLoginRequestDto("invalid@example.com", "password123");

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.empty());

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(tokenService, never()).generateToken(any(UserEntity.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.authUser(payload, response));
            assertEquals("Usuário e/ou senha inválidos.", exception.getMessage());

        }

        @Test
        @DisplayName("Should not be able to login if invalid password")
        void shouldNotBeAbleToLoginIfInvalidPassword() {

            UserLoginRequestDto payload = new UserLoginRequestDto("john.doe@example.com", "wrongPassword");
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(payload.email());
            userEntity.setPassword("encodedPassword123");

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(payload.password(), userEntity.getPassword())).thenReturn(false);

            verify(tokenService, never()).generateToken(any(UserEntity.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.authUser(payload, response));
            assertEquals("Usuário e/ou senha inválidos.", exception.getMessage());
        }

        @Test
        @DisplayName("Should not be able to login if user registration is not confirm")
        void shouldNoBeAbleToLoginIfUserRegistrationIsNotConfirm() {

            UserLoginRequestDto payload = getUserLoginRequestDto();

            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(payload.email());
            userEntity.setPassword(payload.password());

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(payload.password(), userEntity.getPassword())).thenReturn(true);

            verify(tokenService, never()).generateToken(any(UserEntity.class));
            UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.authUser(payload, response));
            assertEquals("Usuário não confirmou seu cadastro por e-mail.", exception.getMessage());
        }

    }

    private static UserCreateRequestDto getUserCreateRequestDto() {
        return new UserCreateRequestDto(
                "John Doe",
                "john.doe@example.com",
                "Test1234"
        );
    }

    private static UserLoginRequestDto getUserLoginRequestDto() {
        return new UserLoginRequestDto(
                "john.doe@example.com",
                "Test1234"
        );
    }

    private static UserValidateRequestDto getUserValidateRequestDto() {
        return new UserValidateRequestDto(
                "john.doe@example.com",
                "A1B2"
        );
    }

    private static UserRevalidateRequestDto getUserRevalidateRequestDto() {
        return new UserRevalidateRequestDto(
                "john.doe@example.com"
        );
    }
}