package com.izi.ecommerce.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenRevocationServiceImplTest {

    private final CacheService cacheService = mock(CacheService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final TokenRevocationServiceImpl service = new TokenRevocationServiceImpl(cacheService, jwtService);

    @Test
    void revokeStoresHashedTokenUntilItExpires() {
        when(jwtService.validateToken("token")).thenReturn(true);
        when(jwtService.getExpirationFromToken("token")).thenReturn(Instant.now().plus(Duration.ofHours(1)));

        service.revoke("token");

        verify(cacheService).put(
                argThat((String key) -> key.startsWith("auth:revoked:") && !key.contains("token")),
                eq(Boolean.TRUE),
                argThat((Duration ttl) -> ttl.compareTo(Duration.ofMinutes(59)) > 0)
        );
    }

    @Test
    void revokeIgnoresInvalidToken() {
        when(jwtService.validateToken("token")).thenReturn(false);

        service.revoke("token");

        verify(cacheService, never()).put(anyString(), any(), any(Duration.class));
    }

    @Test
    void isRevokedChecksCache() {
        when(cacheService.get(anyString(), eq(Boolean.class))).thenReturn(Optional.of(Boolean.TRUE));

        assertThat(service.isRevoked("token")).isTrue();
    }
}
