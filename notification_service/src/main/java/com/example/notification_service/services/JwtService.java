package com.example.notification_service.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for JWT token validation and claims extraction.
 */
@Service
public class JwtService {

    private static final String PROVIDER_CLAIM = "provider";

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validates a JWT token by checking its expiration.
     *
     * @param token the JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getBody();

            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the username from a basic authentication token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String getBaseSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts OAuth subject and provider from token claims.
     *
     * @param token the JWT token
     * @return list containing subject ID and provider name
     */
    public List<String> getOauthSubjects(String token) {
        Claims claims = extractAllClaims(token);
        List<String> subjects = new ArrayList<>();
        subjects.add(claims.getSubject());
        subjects.add((String) claims.get(PROVIDER_CLAIM));
        return subjects;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
