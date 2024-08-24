package personal.investwallet.modules.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import personal.investwallet.exceptions.RecordNotFoundException;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.providers.JWTProvider;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTProvider jwtProvider;

    public UUID createUser(UserCreateRequestDto payload) {

        String password = passwordEncoder.encode(payload.password());

        var newUser = new UserEntity();
        newUser.setName(payload.name());
        newUser.setEmail(payload.email());
        newUser.setPassword(password);
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(null);

        var userSaved = userRepository.save(newUser);

        return userSaved.getId();
    }

    public String authUser(UserLoginRequestDto payload) {

        Optional<UserEntity> user = userRepository.findByEmail(payload.email());
        if (user.isEmpty()) {
            throw new RecordNotFoundException("Usuário e/ou senha inválidos.");
        }

        var isPasswordValid = passwordEncoder.matches(payload.password(), user.get().getPassword());

        if (!isPasswordValid) {
            throw new RecordNotFoundException("Usuário e/ou senha inválidos.");
        }

        String token = jwtProvider.generateToken(user.get());

        return token;
    }

}
