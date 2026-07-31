package com.eneik.generated.knowledgebase;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret:}") String configuredSecret,
                   @Value("${app.jwt.expirationMs:86400000}") long expirationMs) {
        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            this.secret = configuredSecret;
        } else {
            // Generate a cryptographically secure random secret key for this runtime session
            byte[] randomBytes = new byte[32];
            new SecureRandom().nextBytes(randomBytes);
            this.secret = Base64.getEncoder().encodeToString(randomBytes);
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, String role) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withSubject(username)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
                .sign(algorithm);
    }

    public Claims parseToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            String username = jwt.getSubject();
            String role = jwt.getClaim("role").asString();
            if (username == null || role == null) {
                return null;
            }
            return new Claims(username, role);
        } catch (Exception e) {
            return null; // Invalid token
        }
    }

    public static class Claims {
        private final String username;
        private final String role;

        public Claims(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
