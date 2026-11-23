package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.InvalidPasswordException;

import com.izi.ecommerce.model.AuthRequest;
import com.izi.ecommerce.model.UserInfo;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;


@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_RATE_LIMIT_PREFIX = "login:";

    private final AuthenticationManager authenticationManager;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimiterConfig loginRateLimiterConfig;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            RateLimiterRegistry rateLimiterRegistry,
            @Value("${rate.limit.login.attempts:5}") int loginAttempts,
            @Value("${rate.limit.login.period:60}") int loginPeriodInSeconds
    ) {
        this.authenticationManager = authenticationManager;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.loginRateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(loginAttempts)
                .limitRefreshPeriod(Duration.ofSeconds(loginPeriodInSeconds))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @Override
    public UserInfo authenticate(AuthRequest authRequest) {
        checkLoginRateLimit(authRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            UserInfo user = (UserInfo) authentication.getPrincipal();
            return user;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new InvalidPasswordException("Invalid username or password");
        }
    }

    // Limits login attempts per username to slow down password guessing.
    private void checkLoginRateLimit(String username) {
        String key = LOGIN_RATE_LIMIT_PREFIX + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(key, loginRateLimiterConfig);

        if (!rateLimiter.acquirePermission()) {
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }
    }
}
