package com.izi.ecommerce.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private static final String REVOKED_TOKEN_KEY = "auth:revoked:";

    private final CacheService cacheService;
    private final JwtService jwtService;

    @Override
    public void revoke(String token) {
        if (!jwtService.validateToken(token)) {
            return;
        }

        // Keep the entry only until the token would have expired anyway.
        Duration ttl = Duration.between(Instant.now(), jwtService.getExpirationFromToken(token));
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        cacheService.put(getKey(token), Boolean.TRUE, ttl);
    }

    @Override
    public boolean isRevoked(String token) {
        return cacheService.get(getKey(token), Boolean.class).isPresent();
    }

    // Store a hash instead of the raw token so Redis never holds usable credentials.
    private String getKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return REVOKED_TOKEN_KEY + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
