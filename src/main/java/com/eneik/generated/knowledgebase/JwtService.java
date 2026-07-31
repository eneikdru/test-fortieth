package com.eneik.generated.knowledgebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;
    private final ObjectMapper objectMapper;
    private static final String ALGORITHM = "HmacSHA256";

    public JwtService(@Value("${jwt.secret:super-secret-key-for-jwt-signing-12345678901234567890}") String secret) {
        this.secret = secret;
        this.objectMapper = new ObjectMapper();
    }

    public String generateToken(String username, String role) {
        try {
            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("alg", "HS256");
            headerMap.put("typ", "JWT");

            long now = System.currentTimeMillis();
            long exp = now + 3600000; // 1 hour expiration

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sub", username);
            payloadMap.put("role", role);
            payloadMap.put("exp", exp);

            String headerJson = objectMapper.writeValueAsString(headerMap);
            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signatureInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signatureInput);

            return signatureInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT", e);
        }
    }

    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        String expectedSignature = sign(header + "." + payload);
        if (!expectedSignature.equals(signature)) {
            return null; // Signature mismatch
        }

        try {
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claimsMap = objectMapper.readValue(decodedPayload, Map.class);

            String sub = (String) claimsMap.get("sub");
            String role = (String) claimsMap.get("role");
            Number expNum = (Number) claimsMap.get("exp");

            if (sub == null || role == null || expNum == null) {
                return null;
            }

            long exp = expNum.longValue();
            if (System.currentTimeMillis() > exp) {
                return null; // Token expired
            }

            return new Claims(sub, role);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error signing JWT", e);
        }
    }

    public static class Claims {
        private final String username;
        private final String role;

        public Claims(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}
