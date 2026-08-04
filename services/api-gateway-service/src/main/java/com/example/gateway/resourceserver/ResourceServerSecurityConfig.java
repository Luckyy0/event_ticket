package com.example.gateway.resourceserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reference documentation and guide for downstream Spring Boot Resource Servers
 * (e.g. order-service, inventory-service, payment-service, ticket-service).
 *
 * Pattern for downstream Servlet-based microservices:
 *
 * <pre>
 * &#64;Configuration
 * &#64;EnableWebSecurity
 * &#64;EnableMethodSecurity
 * public class ResourceServerConfig {
 *
 *     private final JwtAuthConverter jwtAuthConverter;
 *
 *     public ResourceServerConfig(JwtAuthConverter jwtAuthConverter) {
 *         this.jwtAuthConverter = jwtAuthConverter;
 *     }
 *
 *     &#64;Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         return http
 *                 .csrf(AbstractHttpConfigurer::disable)
 *                 .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
 *                 .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
 *                 .authorizeHttpRequests(auth -> auth
 *                         .requestMatchers("/actuator/health/**", "/actuator/info/**").permitAll()
 *                         .requestMatchers("/api/v1/payments/webhook/**").permitAll()
 *                         .anyRequest().authenticated()
 *                 )
 *                 .build();
 *     }
 * }
 * </pre>
 */
@Configuration
public class ResourceServerSecurityConfig {
    // Reference Pattern Holder
}
