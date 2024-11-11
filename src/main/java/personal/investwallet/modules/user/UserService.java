package personal.investwallet.modules.user;

import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.UnauthorizedException;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.modules.user.dto.UserRevalidateRequestDto;
import personal.investwallet.modules.user.dto.UserValidateRequestDto;
import personal.investwallet.security.TokenService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TokenService tokenService;

    private final Logger log = LogManager.getLogger();

    public String createUser(UserCreateRequestDto payload) {

        Optional<UserEntity> user = userRepository.findByEmail(payload.email());

        if (user.isPresent())
            throw new ConflictException("Usuário já existe");

        String password = passwordEncoder.encode(payload.password());

        var newUser = new UserEntity();
        newUser.setName(payload.name());
        newUser.setEmail(payload.email());
        newUser.setPassword(password);
        newUser.setCreatedAt(Instant.now());

        userRepository.insert(newUser);

        log.info("Usuário %s cadastrado com sucesso!".formatted(payload.name()));

        return "Usuário cadastrado com sucesso";
    }

    public String validateUser(UserValidateRequestDto payload) {

        userRepository.findByEmail(payload.email())
                .orElseThrow(() -> new UnauthorizedException("Email inválido"));

        Cache cache = cacheManager.getCache("verificationCodes");

        if (cache != null) {
            String cachedCode = cache.get(payload.email(), String.class);

            if (cachedCode != null && cachedCode.equals(payload.code())) {
                userRepository.updateCheckedAsTrueByEmail(payload.email(), Instant.now());
                cache.evict(payload.email());
                return "Validação concluída com sucesso";
            } else {
                throw new UnauthorizedException("O código informado não confere");
            }
        } else {
            throw new UnauthorizedException("Tempo de validação expirado");
        }
    }

    public void verifyExistingUserAndVerificationCode(UserRevalidateRequestDto payload) {

        UserEntity user = userRepository.findByEmail(payload.email())
                .orElseThrow(() -> new UnauthorizedException("Email inválido"));

        if (user.isChecked())
            throw new ConflictException("O cadastro do usuário já está válido");

        Cache cache = cacheManager.getCache("verificationCodes");

        if (cache != null) {
            String cachedCode = cache.get(payload.email(), String.class);

            if (cachedCode != null)
                throw new ConflictException("O código de verificação enviado anteriormente ainda está válido");

        }
    }

    public String authUser(UserLoginRequestDto payload, HttpServletResponse response) {

        UserEntity user = userRepository.findByEmail(payload.email())
                .orElseThrow(() -> new UnauthorizedException("Usuário e/ou senha inválidos"));

        boolean isPasswordValid = passwordEncoder.matches(payload.password(), user.getPassword());

        if (!isPasswordValid)
            throw new UnauthorizedException("Usuário e/ou senha inválidos");

        if (!user.isChecked())
            throw new UnauthorizedException("Usuário não confirmou seu cadastro por e-mail");

        String token = tokenService.generateToken(user);
        tokenService.addTokenToCookies(token, response);

        return token;
    }

}
