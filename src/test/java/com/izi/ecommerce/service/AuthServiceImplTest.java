package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.InvalidPasswordException;
import com.izi.ecommerce.model.AuthRequest;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private AuthenticationManager authenticationManager;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        authService = new AuthServiceImpl(authenticationManager, RateLimiterRegistry.ofDefaults(), 3, 60);
    }

    @Test
    void blocksLoginAfterTooManyAttemptsForSameUsername() {
        AuthRequest request = new AuthRequest("buyer", "wrong-password");

        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidPasswordException.class, () -> authService.authenticate(request));
        }

        assertThrows(RequestNotPermitted.class, () -> authService.authenticate(request));
        verify(authenticationManager, times(3)).authenticate(any());
    }

    @Test
    void usernameLimitIsCaseInsensitive() {
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidPasswordException.class,
                    () -> authService.authenticate(new AuthRequest("Buyer", "wrong-password")));
        }

        assertThrows(RequestNotPermitted.class,
                () -> authService.authenticate(new AuthRequest("buyer", "wrong-password")));
    }

    @Test
    void otherUsernamesAreNotBlocked() {
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidPasswordException.class,
                    () -> authService.authenticate(new AuthRequest("buyer", "wrong-password")));
        }

        assertThrows(InvalidPasswordException.class,
                () -> authService.authenticate(new AuthRequest("someone-else", "wrong-password")));
    }
}
