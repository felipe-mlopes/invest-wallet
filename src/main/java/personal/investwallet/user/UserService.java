package personal.investwallet.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import personal.investwallet.common.exceptions.RecordNotFoundException;
import personal.investwallet.user.dto.UserCreateDto;
import personal.investwallet.user.dto.UserLoginDto;

@Service
public class UserService {

    private UserRepository userRepository;

    private PasswordEncoder encoder;

    public UUID createUser(UserCreateDto payload) {

        var newUser = new UserEntity();
        newUser.setUserId(UUID.randomUUID());
        newUser.setName(payload.name());
        newUser.setEmail(payload.email());
        newUser.setPassword(encoder.encode(payload.password()));
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(null);

        var userSaved = userRepository.save(newUser);

        return userSaved.getUserId();
    }

    public String authUser(UserLoginDto payload) {

        Optional<UserEntity> user = userRepository.findByEmail(payload.email());

        if (user.isEmpty()) {
            throw new RecordNotFoundException("Usuário e/ou senha inválidos.");
        }

        var isPasswordValid = encoder.matches(payload.password(), user.get().getPassword());

        if (!isPasswordValid) {
            throw new RecordNotFoundException("Usuário e/ou senha inválidos.");
        }

        return "Usuário autorizado";
    }
    
}
