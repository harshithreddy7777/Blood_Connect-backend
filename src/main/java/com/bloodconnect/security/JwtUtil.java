package com.bloodconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                // Pad to at least 32 bytes for HMAC-SHA-256 requirement
                byte[] padded = new byte[32];
                System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
                return Keys.hmacShaKeyFor(padded);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(String username, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return createToken(claims, username, expiration);
    }

    public String generateTokenWithCustomExpiration(String username, String role, Long userId, long customExpirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return createToken(claims, username, customExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expDuration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expDuration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp != null && exp.before(new Date());
    }

    public boolean validateToken(String token, String username) {
        final String extractedUser = extractUsername(token);
        return (extractedUser != null && extractedUser.equals(username) && !isTokenExpired(token));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
