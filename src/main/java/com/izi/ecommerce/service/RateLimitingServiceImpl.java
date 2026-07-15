package com.izi.ecommerce.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingServiceImpl implements RateLimitingService {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public <T> T executeWithRateLimit(String key, Supplier<T> operation) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(key);
        return RateLimiter.decorateSupplier(rateLimiter, operation).get();
    }
}
