package personal.investwallet.providers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;

import personal.investwallet.modules.user.UserEntity;

@Service
public class JWTProvider {

    @Value("${security.token.secret}")
    private String secretKey;

    public String generateToken(UserEntity user) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        try {
            String token = JWT.create()
                    .withIssuer("user-auth")
                    .withSubject(user.getId().toString())
                    .withExpiresAt(this.generateExpirationDate())
                    .sign(algorithm);

            return token;
        } catch (JWTCreationException ex) {
            throw new RuntimeException("Error while authentication");
        }
    }

    public String validateToken(String token) {

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        try {
            String tokenDecoded = JWT.require(algorithm)
                    .withIssuer("user-auth")
                    .build()
                    .verify(token)
                    .getSubject();

            return tokenDecoded;
        } catch (JWTVerificationException ex) {
            ex.printStackTrace();

            return null;
        }
    }

    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("UTC-3"));
    }
}
