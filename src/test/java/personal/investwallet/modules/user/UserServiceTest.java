package personal.investwallet.modules.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.exceptions.UserAlreadyExistsException;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.security.TokenService;

import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

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

            assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(payload));
            verify(userRepository, never()).insert(any(UserEntity.class));
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

            UserLoginRequestDto payload = getUserLoginRequestDto();

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(payload.password(),userEntity.getPassword())).thenReturn(true);
            when(tokenService.generateToken(userEntity)).thenReturn("generatedToken123");

            String result = userService.authUser(payload);

            verify(userRepository, times(1)).findByEmail(payload.email());
            verify(passwordEncoder, times(1)).matches(payload.password(), userEntity.getPassword());
            verify(tokenService, times(1)).generateToken(userEntity);
            assertEquals("generatedToken123", result);
        }

        @Test
        @DisplayName("Should not be able to login if invalid email")
        void shouldNotBeAbleToLoginIfInvalidEmail() {

            UserLoginRequestDto payload = new UserLoginRequestDto("invalid@example.com", "password123");

            when(userRepository.findByEmail(payload.email())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.authUser(payload));

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(tokenService, never()).generateToken(any(UserEntity.class));
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

            assertThrows(ResourceNotFoundException.class, () -> userService.authUser(payload));

            verify(tokenService, never()).generateToken(any(UserEntity.class));
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
}