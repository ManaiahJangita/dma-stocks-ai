package com.stockai.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-time-seconds:3600}")
    private long expirationTimeSeconds;

    @PostConstruct
    public void validateConfiguration() {
        signingKey();
        if (expirationTimeSeconds <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_SECONDS must be greater than zero");
        }
    }

    public String generateToken(String username, String userId, List<String> authorities) {
        Date issuedAt = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("authorities", authorities)
                .setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + expirationTimeSeconds * 1000L))
                .signWith(SignatureAlgorithm.HS256, signingKey())
                .compact();
    }

    public int getCookieMaxAgeSeconds() {
        return (int) Math.min(Integer.MAX_VALUE, expirationTimeSeconds);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject() != null && !claims.getSubject().isBlank();
        } catch (Exception e) {
            log.debug("Rejected invalid JWT", e);
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return parseClaims(token).get("uid", String.class);
    }

    public List<String> extractAuthorities(String token) {
        Object claim = parseClaims(token).get("authorities");
        if (!(claim instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    public LocalDateTime extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(expiration.getTime()), ZoneOffset.UTC);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey())
                .parseClaimsJws(token)
                .getBody();
    }

    private byte[] signingKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must not be blank");
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(secretKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}