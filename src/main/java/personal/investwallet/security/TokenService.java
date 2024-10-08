package personal.investwallet.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;

import personal.investwallet.modules.user.UserEntity;

@Service
public class TokenService {
    @Value("${security.token.secret}")
    private String secret;

    public String generateToken(UserEntity user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("login-auth-api")
                    .withSubject(user.getId())
                    .withExpiresAt(this.generateExpirationDate())
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while authenticating");
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("login-auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    public String extractUserIdFromToken(String token) {

        Algorithm algorithm = Algorithm.HMAC256(secret);

        DecodedJWT decodedJWT = JWT.require(algorithm)
                .build()
                .verify(token);

        return decodedJWT.getClaim("sub").asString();
    }

    public void addTokenToCookies(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", token);

        cookie.setMaxAge(24 * 60 * 60); // 1 day
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
    }
}