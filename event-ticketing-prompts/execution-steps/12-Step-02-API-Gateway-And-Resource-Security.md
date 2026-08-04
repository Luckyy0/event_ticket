# STEP 02 — API GATEWAY AND RESOURCE SERVICE SECURITY

## Objective

Set up the API Gateway (Spring Cloud Gateway) with JWT validation, routing, rate limiting, header sanitization, and Correlation ID injection. Then establish the OAuth2 Resource Server configuration pattern that every downstream business service will reuse, including method-level security and ownership authorization.

---

## Prerequisites

- Step 01 completed (Keycloak running, BFF Service functional, realm-test.json available).
- docker-compose.yml includes Keycloak.

---

## Task 1: API Gateway — Project Setup

### 1.1 Initialize Project

Create `services/api-gateway-service/` with:
- `build.gradle.kts` — dependencies: Spring Cloud Gateway, Spring Security OAuth2 Resource Server, Spring Boot Actuator, Resilience4j, Testcontainers, JUnit 5, AssertJ.
- `application.yml` — route definitions, JWT issuer-uri, rate limit config, timeout config.
- `application-test.yml` — overrides for Testcontainers dynamic ports.

### 1.2 Package Structure

```
api-gateway-service/src/main/java/com/example/gateway/
├── config/
│   ├── SecurityConfig.java
│   ├── RouteConfig.java
│   ├── RateLimitConfig.java
│   └── CorsConfig.java
├── filter/
│   ├── CorrelationIdFilter.java
│   ├── HeaderSanitizationFilter.java
│   └── RequestLoggingFilter.java
└── GatewayApplication.java
```

---

## Task 2: Gateway — JWT Validation (TDD)

### 2.1 Acceptance Criteria

```
Given a request arrives with a valid JWT in the Authorization header
When the Gateway processes the request
Then JWT signature is validated against Keycloak JWKS
And issuer is validated
And expiration is checked
And the request is forwarded to the downstream service

Given a request arrives without an Authorization header
When the Gateway processes the request to a protected route
Then the Gateway returns 401 Unauthorized

Given a request arrives with an expired JWT
When the Gateway processes the request
Then the Gateway returns 401 Unauthorized

Given a request arrives with a JWT signed by a different key
When the Gateway processes the request
Then the Gateway returns 401 Unauthorized
```

### 2.2 Test Cases (RED)

```java
// JWT Validation
shouldForwardRequest_whenTokenIsValid()
shouldReturn401_whenNoTokenProvided()
shouldReturn401_whenTokenIsExpired()
shouldReturn401_whenTokenHasInvalidSignature()
shouldReturn401_whenIssuerDoesNotMatch()
shouldReturn401_whenAudienceDoesNotMatch()
shouldReturn401_whenTokenIsMalformed()
shouldRefreshJwks_whenUnknownKidEncountered()
```

### 2.3 Production Code (GREEN)

Implement:
- `SecurityConfig` — OAuth2 Resource Server with JWT decoder, JWKS URI from Keycloak.
- JWT issuer validation, audience validation.

---

## Task 3: Gateway — Routing Configuration

### 3.1 Route Definitions

| Route ID | Path Pattern | Target Service | Auth Required |
|---|---|---|---|
| catalog-read | `/api/v1/events/**` | catalog-service | No (public read) |
| inventory | `/api/v1/inventory/**` | inventory-service | Yes |
| orders | `/api/v1/orders/**` | order-service | Yes |
| payments | `/api/v1/payments/**` | payment-service | Yes |
| payment-webhook | `/api/v1/payments/webhook/**` | payment-service | No (verified by HMAC) |
| tickets | `/api/v1/tickets/**` | ticket-service | Yes |
| checkin | `/api/v1/checkin/**` | check-in-service | Yes |
| user-profile | `/api/v1/users/**` | user-profile-service | Yes |
| admin | `/api/v1/admin/**` | various | Yes (ADMIN role) |

### 3.2 Test Cases (RED)

```java
shouldRouteToCorrectService_whenPathMatches()
shouldAllowPublicAccess_toCatalogReadEndpoints()
shouldRequireAuthentication_forInventoryEndpoints()
shouldAllowPublicAccess_toPaymentWebhookEndpoint()
shouldReturn404_whenRouteDoesNotExist()
```

---

## Task 4: Gateway — Correlation ID (TDD)

### 4.1 Acceptance Criteria

```
Given a request arrives without X-Correlation-Id header
When the Gateway processes it
Then the Gateway generates a new UUID and adds X-Correlation-Id to the request
And includes X-Correlation-Id in the response headers

Given a request arrives with an existing X-Correlation-Id header
When the Gateway processes it
Then the Gateway preserves the existing Correlation ID
And forwards it to the downstream service
And includes it in the response headers
```

### 4.2 Test Cases (RED)

```java
shouldGenerateCorrelationId_whenNotProvided()
shouldPreserveCorrelationId_whenAlreadyPresent()
shouldIncludeCorrelationId_inResponseHeaders()
shouldForwardCorrelationId_toDownstreamService()
```

### 4.3 Production Code (GREEN)

Implement `CorrelationIdFilter` as a Spring Cloud Gateway GlobalFilter.

---

## Task 5: Gateway — Header Sanitization (TDD)

### 5.1 Acceptance Criteria

```
Given a client sends a request with spoofed identity headers
  (X-User-Id, X-Roles, X-User-Email, X-Authenticated-User)
When the Gateway processes the request
Then these headers are stripped before forwarding to downstream services
And only headers derived from the validated JWT are forwarded
```

### 5.2 Test Cases (RED)

```java
shouldStripSpoofedUserIdHeader_beforeForwarding()
shouldStripSpoofedRolesHeader_beforeForwarding()
shouldStripSpoofedEmailHeader_beforeForwarding()
shouldNotStripLegitimateHeaders_likeContentType()
shouldNotStripCorrelationIdHeader()
```

### 5.3 Production Code (GREEN)

Implement `HeaderSanitizationFilter` — strips a configurable list of forbidden headers.

---

## Task 6: Gateway — Rate Limiting (TDD)

### 6.1 Acceptance Criteria

```
Given rate limit is configured at 100 requests per minute per IP
When a client sends 101 requests within 1 minute
Then the first 100 requests are processed normally
And the 101st request returns 429 Too Many Requests
And the response includes Retry-After header

Given rate limit is configured for Flash Sale reservation endpoint
When burst traffic arrives at /api/v1/inventory/reservations
Then the rate limit applies per-user (extracted from JWT subject)
And unauthenticated requests use per-IP rate limiting
```

### 6.2 Test Cases (RED)

```java
shouldAllowRequests_withinRateLimit()
shouldReturn429_whenRateLimitExceeded()
shouldIncludeRetryAfterHeader_whenRateLimited()
shouldApplyPerUserRateLimit_forAuthenticatedRequests()
shouldApplyPerIpRateLimit_forUnauthenticatedRequests()
shouldApplyStricterRateLimit_forReservationEndpoint()
```

### 6.3 Production Code (GREEN)

Implement rate limiting using Spring Cloud Gateway's built-in `RequestRateLimiter` with Redis backend, or Resilience4j RateLimiter.

---

## Task 7: Gateway — Timeout and Circuit Breaker

### 7.1 Configuration

| Setting | Value |
|---|---|
| Global connect timeout | 3 seconds |
| Global response timeout | 10 seconds |
| Inventory reservation timeout | 5 seconds |
| Circuit breaker failure rate threshold | 50% |
| Circuit breaker slow call duration | 5 seconds |
| Circuit breaker wait duration in open state | 30 seconds |

### 7.2 Test Cases (RED)

```java
shouldReturn504_whenDownstreamServiceTimesOut()
shouldOpenCircuitBreaker_whenFailureRateExceedsThreshold()
shouldReturn503_whenCircuitBreakerIsOpen()
shouldCloseCircuitBreaker_afterWaitDuration()
```

---

## Task 8: Resource Server Security Pattern (Reusable)

### 8.1 Objective

Create a reusable security configuration pattern that every downstream business service will use. This is NOT a shared library — each service copies and adapts the pattern.

### 8.2 Reference Implementation

Create a reference in `services/inventory-service/` (or a documentation file) showing:

**SecurityConfig.java** pattern:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API-only, no browser session
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**JwtAuthConverter** pattern — extract roles from Keycloak JWT:
```java
// Extract realm_access.roles from JWT claims
// Map to Spring Security GrantedAuthority
// Support both realm roles and client roles
```

**Ownership authorization** pattern:
```java
@PreAuthorize("hasRole('CUSTOMER')")
public OrderResponse getOrder(UUID orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    if (!order.getUserId().equals(currentUser.getId())) {
        throw new AccessDeniedException("Not the owner of this order");
    }
    return mapper.toResponse(order);
}
```

### 8.3 Test Cases (RED)

```java
// Resource Server security tests (pattern for all services)
shouldReturn200_whenTokenHasRequiredRole()
shouldReturn401_whenNoTokenProvided()
shouldReturn401_whenTokenIsExpired()
shouldReturn403_whenTokenLacksRequiredRole()
shouldReturn403_whenTokenLacksRequiredScope()
shouldReturn403_whenUserIsNotResourceOwner()
shouldAllowHealthEndpoint_withoutAuthentication()
shouldAllowWebhookEndpoint_withoutAuthentication()
shouldRejectRequest_whenIssuerDoesNotMatch()
shouldExtractRealmRoles_fromKeycloakJwt()
shouldExtractClientRoles_fromKeycloakJwt()
```

---

## Task 9: Integration Tests with Keycloak Testcontainer

### 9.1 Gateway Integration Tests

```java
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
class GatewaySecurityIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
        .withRealmImportFile("realm-test.json");

    // Obtain real tokens from Keycloak using Resource Owner Password Grant (test only)
    // Use tokens to test Gateway JWT validation end-to-end
}
```

### 9.2 Integration Test Cases

```java
shouldAcceptValidToken_fromRealKeycloak()
shouldRejectExpiredToken_fromRealKeycloak()
shouldRejectTokenFromDifferentRealm()
shouldHandleJwksKeyRotation()
shouldStripSpoofedHeaders_inEndToEndFlow()
shouldApplyRateLimit_inEndToEndFlow()
shouldReturnCorrelationId_inEndToEndFlow()
```

---

## Task 10: Configuration Files

Produce complete and runnable:
- `build.gradle.kts`
- `application.yml` (routes, JWT config, rate limit, timeout, circuit breaker)
- `application-test.yml`
- `Dockerfile`

---

## Completion Checklist

- [ ] Gateway project initialized with correct dependencies.
- [ ] JWT validation works against Keycloak JWKS.
- [ ] Routes configured for all services.
- [ ] Public endpoints (catalog read, webhook) accessible without token.
- [ ] Protected endpoints return 401 without token.
- [ ] Expired/invalid tokens return 401.
- [ ] Correlation ID generated and propagated.
- [ ] Spoofed identity headers stripped.
- [ ] Rate limiting works (per-IP and per-user).
- [ ] Timeout and circuit breaker configured.
- [ ] Resource Server pattern documented with code examples.
- [ ] Ownership authorization pattern documented.
- [ ] All unit tests written first (RED) then passed (GREEN).
- [ ] Integration tests pass against Keycloak Testcontainer.
- [ ] Configuration files complete and runnable.
- [ ] No tokens or secrets in logs.
- [ ] Failure scenarios documented.
- [ ] Trade-offs explained.
