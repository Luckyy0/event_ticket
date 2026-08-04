package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class RateLimitConfig {

    /**
     * Primary KeyResolver:
     * If user is authenticated, resolves to user principal name (e.g. userId / username).
     * If unauthenticated, falls back to remote IP address.
     */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.defer(() -> {
                    var remoteAddress = exchange.getRequest().getRemoteAddress();
                    if (remoteAddress != null && remoteAddress.getAddress() != null) {
                        return Mono.just(remoteAddress.getAddress().getHostAddress());
                    }
                    return Mono.just("anonymous-unknown-ip");
                }));
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("unknown-ip");
        };
    }

    /**
     * Standard Rate Limiter: 100 req/sec, burst capacity 150
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(100, 150, 1);
    }

    /**
     * Stricter Rate Limiter for Flash Sale reservations: 10 req/sec, burst capacity 20
     */
    @Bean
    public RedisRateLimiter reservationRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
