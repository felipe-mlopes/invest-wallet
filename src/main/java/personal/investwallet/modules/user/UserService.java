package personal.investwallet.modules.user;

import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.UnauthorizedException;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.security.TokenService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    public String createUser(UserCreateRequestDto payload) {

        Optional<UserEntity> user = userRepository.findByEmail(payload.email());

        if (user.isPresent()) {
            throw new ConflictException("Usuário já existe.");
        }

        String password = passwordEncoder.encode(payload.password());

        var newUser = new UserEntity();
        newUser.setName(payload.name());
        newUser.setEmail(payload.email());
        newUser.setPassword(password);
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(null);

        userRepository.insert(newUser);

        return "Usuário cadastrado com sucesso.";
    }

    public String authUser(UserLoginRequestDto payload, HttpServletResponse response) {

        UserEntity user = userRepository.findByEmail(payload.email())
                .orElseThrow(() -> new UnauthorizedException("Usuário e/ou senha inválidos."));

        var isPasswordValid = passwordEncoder.matches(payload.password(), user.getPassword());

        if (!isPasswordValid) {
            throw new UnauthorizedException("Usuário e/ou senha inválidos.");
        }

        String token = tokenService.generateToken(user);

        tokenService.addTokenToCookies(token, response);

        return token;
    }

}
