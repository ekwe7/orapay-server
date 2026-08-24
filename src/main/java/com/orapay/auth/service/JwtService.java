package com.orapay.auth.service;

import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public interface JwtService {

    String extractUsername(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    String generateToken(UUID userId, String email);

    String generateToken(Map<String, Object> extraClaims, String subject);

    boolean isTokenValid(String token, String userEmail);

    long getExpirationInSeconds();
}
