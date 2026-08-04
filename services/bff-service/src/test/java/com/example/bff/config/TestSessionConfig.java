package com.example.bff.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;

import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class TestSessionConfig {

    @Bean
    @Primary
    public SessionRepository<?> sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }
}
