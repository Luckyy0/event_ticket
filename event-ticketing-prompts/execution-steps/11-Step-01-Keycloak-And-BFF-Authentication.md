# STEP 01 — KEYCLOAK AND BFF AUTHENTICATION

## Objective

Set up Keycloak as the Identity Provider, build the BFF Service implementing Authorization Code Flow with PKCE, establish server-side session management, and verify the entire authentication lifecycle with Testcontainers.

---

## Prerequisites

- Step 00 completed (architecture documents, docker-compose.yml, ADRs).
- docker-compose.yml includes Keycloak and Redis (or in-memory session store).

---

## Task 1: Keycloak Realm Configuration

### 1.1 Create Realm Configuration File

File: `infrastructure/keycloak/realm-dev.json`

Create a Keycloak realm export JSON containing:

| Item | Value |
|---|---|
| Realm name | `event-ticketing` |
| Login theme | Default |
| Token lifespan (Access Token) | 5 minutes |
| Token lifespan (Refresh Token) | 30 minutes |
| Refresh Token rotation | Enabled |

### 1.2 Create Clients

**BFF Client** (confidential):

| Setting | Value |
|---|---|
| Client ID | `bff-client` |
| Client authentication | ON (confidential) |
| Authorization Code Flow | Enabled |
| PKCE | Required (S256) |
| Valid redirect URIs | `http://localhost:8080/api/auth/callback` |
| Post-logout redirect URIs | `http://localhost:8080` |
| Web origins | `http://localhost:3000` |

**API Client** (bearer-only, for resource servers):

| Setting | Value |
|---|---|
| Client ID | `api-client` |
| Client authentication | OFF (public, bearer-only) |
| Direct Access Grants | Disabled |

### 1.3 Create Roles

Realm roles: `CUSTOMER`, `EVENT_ORGANIZER`, `STAFF`, `ADMIN`, `SUPPORT`.

### 1.4 Create Scopes

Client scopes: `event:read`, `event:write`, `inventory:read`, `inventory:manage`, `order:read`, `order:create`, `order:cancel`, `payment:create`, `payment:refund`, `ticket:read`, `ticket:checkin`.

### 1.5 Create Test Users

| Username | Password | Roles | Purpose |
|---|---|---|---|
| `customer1` | `test1234` | CUSTOMER | Standard customer |
| `organizer1` | `test1234` | EVENT_ORGANIZER | Event creator |
| `staff1` | `test1234` | STAFF | Check-in staff |
| `admin1` | `test1234` | ADMIN | System admin |

### 1.6 Create Test Realm

File: `infrastructure/keycloak/realm-test.json`

Same structure as `realm-dev.json` but with:
- Redirect URIs pointing to test ports.
- Shorter token lifetimes for faster test execution.
- All test users pre-created.

---

## Task 2: BFF Service — Project Setup

### 2.1 Initialize Project

Create `services/bff-service/` with:
- `build.gradle.kts` — dependencies: Spring Boot Web, Spring Security OAuth2 Client, Spring Session (Redis or in-memory), Spring Boot Actuator, Testcontainers, JUnit 5, AssertJ.
- `application.yml` — Keycloak provider configuration, session settings, CSRF settings.
- `application-test.yml` — overrides for Testcontainers dynamic ports.

### 2.2 Package Structure

```
bff-service/src/main/java/com/example/bff/
├── config/
│   ├── SecurityConfig.java
│   ├── SessionConfig.java
│   └── CsrfConfig.java
├── controller/
│   ├── AuthController.java
│   └── SessionController.java
├── service/
│   ├── TokenService.java
│   └── SessionService.java
├── filter/
│   └── CsrfTokenFilter.java
└── BffApplication.java
```

---

## Task 3: BFF Authentication Endpoints (TDD)

### 3.1 Acceptance Criteria

```
Given a user is not logged in
When they call GET /api/auth/login
Then BFF generates state, nonce, PKCE code_verifier and code_challenge
And stores state, nonce, code_verifier in server-side session
And redirects to Keycloak authorization endpoint with correct parameters

Given Keycloak redirects back with a valid authorization code and matching state
When BFF receives the callback at GET /api/auth/callback
Then BFF validates state matches session
And exchanges authorization_code + code_verifier for tokens
And validates nonce in ID Token
And stores Access Token and Refresh Token in server-side session
And sets an HttpOnly, Secure, SameSite session cookie
And redirects to the frontend application

Given a user has an active session
When they call POST /api/auth/logout with valid CSRF token
Then BFF invalidates the server-side session
And calls Keycloak end_session_endpoint
And clears the session cookie

Given a user has an active session with an expired Access Token
When they make any API request
Then BFF uses the Refresh Token to obtain a new Access Token from Keycloak
And updates the session with the new tokens
And forwards the request with the new Access Token
```

### 3.2 Test Cases — Write These FIRST (RED)

```java
// Auth Controller Tests
shouldRedirectToKeycloak_whenLoginRequested()
shouldIncludePkceCodeChallenge_whenLoginRequested()
shouldIncludeStateAndNonce_whenLoginRequested()
shouldStoreStateAndNonceInSession_whenLoginRequested()

// Callback Tests
shouldExchangeCodeForTokens_whenCallbackReceivedWithValidState()
shouldSetHttpOnlySessionCookie_whenCallbackSucceeds()
shouldRejectCallback_whenStateMismatch()
shouldRejectCallback_whenNonceMismatch()
shouldRejectCallback_whenAuthorizationCodeMissing()
shouldRejectCallback_whenKeycloakReturnsError()

// Session Tests
shouldReturnUserInfo_whenSessionIsActive()
shouldReturn401_whenSessionIsExpired()
shouldReturn401_whenNoCookieProvided()

// Token Refresh Tests
shouldRefreshAccessToken_whenTokenIsExpired()
shouldInvalidateSession_whenRefreshTokenIsExpired()
shouldInvalidateSession_whenRefreshFails()

// Logout Tests
shouldInvalidateSession_whenLogoutRequested()
shouldCallKeycloakEndSession_whenLogoutRequested()
shouldClearCookie_whenLogoutRequested()
shouldRequireCsrfToken_whenLogoutRequested()
shouldReject403_whenCsrfTokenMissing()

// Cookie Security Tests
shouldSetHttpOnlyFlag_onSessionCookie()
shouldSetSecureFlag_onSessionCookie_inProductionProfile()
shouldSetSameSiteAttribute_onSessionCookie()

// Keycloak Unavailable Tests
shouldReturn503_whenKeycloakIsUnavailable_duringLogin()
shouldReturn503_whenKeycloakIsUnavailable_duringCallback()
```

### 3.3 Production Code (GREEN)

After all tests are written and confirmed RED, implement:
- `AuthController` — login, callback, logout endpoints.
- `SecurityConfig` — Spring Security OAuth2 client configuration, CSRF configuration, session management.
- `TokenService` — token exchange, refresh logic.
- `SessionService` — session creation, retrieval, invalidation.
- `CsrfTokenFilter` — CSRF token in response header or cookie for SPA consumption.

### 3.4 Refactor

- Extract PKCE generation into a utility.
- Extract cookie configuration into a dedicated config class.
- Ensure all security-sensitive operations are logged (without logging token values).

---

## Task 4: BFF Proxy to Backend

### 4.1 Acceptance Criteria

```
Given a user has an active session with a valid Access Token
When they call GET /api/v1/events (proxied through BFF)
Then BFF attaches Bearer Access Token to the outbound request
And forwards the request to the API Gateway
And returns the response to the browser

Given a user has an active session but Access Token is expired
When they call any API endpoint through BFF
Then BFF refreshes the token first
And then forwards the request with the new token

Given a user calls a state-changing endpoint (POST, PUT, DELETE)
When CSRF token is missing or invalid
Then BFF returns 403 Forbidden
```

### 4.2 Test Cases (RED)

```java
shouldForwardRequestWithBearerToken_whenSessionIsActive()
shouldRefreshTokenBeforeForwarding_whenAccessTokenExpired()
shouldReturn403_whenCsrfTokenMissing_onPostRequest()
shouldReturn401_whenNoSessionExists()
shouldPropagateCorrelationId_whenForwardingRequest()
```

### 4.3 Production Code (GREEN)

Implement the proxy/gateway logic in BFF that:
- Resolves session from cookie.
- Attaches Bearer token.
- Generates or propagates Correlation ID.
- Forwards to API Gateway.
- Returns response to browser.

---

## Task 5: Integration Tests with Keycloak Testcontainer

### 5.1 Test Infrastructure

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BffAuthIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
        .withRealmImportFile("realm-test.json");

    @DynamicPropertySource
    static void configureKeycloak(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri",
            () -> keycloak.getAuthServerUrl() + "/realms/event-ticketing");
        // ... other dynamic properties
    }
}
```

### 5.2 Integration Test Cases

```java
// Full flow integration tests against real Keycloak
shouldCompleteFullLoginFlow_withRealKeycloak()
shouldRefreshTokenSuccessfully_withRealKeycloak()
shouldLogoutAndInvalidateSession_withRealKeycloak()
shouldRejectExpiredToken_withRealKeycloak()
shouldRejectInvalidIssuer_withRealKeycloak()
shouldHandleKeycloakRestart_gracefully()
```

---

## Task 6: Configuration Files

Produce complete and runnable:
- `build.gradle.kts`
- `application.yml`
- `application-test.yml`
- `Dockerfile`
- Updated `docker-compose.yml` entry for bff-service (if needed).

---

## Task 7: Advanced Identity Features

### 7.1 LDAP Integration (Enterprise Users)
- Configure Keycloak User Federation to mock an LDAP dataset (e.g., using an LDAP Testcontainer like osixia/openldap).
- Ensure Enterprise users (LDAP) cannot change their passwords or roles.
- Ensure self-registered users can change passwords and have the default role `CUSTOMER`.

### 7.2 Admin Role Management API
- Create an API endpoint in the BFF (or proxy to an Identity Service) that allows users with the `ADMIN` role to change other users' roles.
- Use the **Client Credentials Flow** (client secret) via the Keycloak Admin REST API to execute these role changes securely.

---

## Completion Checklist

- [ ] Keycloak realm-dev.json and realm-test.json created.
- [ ] BFF project initialized with correct dependencies.
- [ ] All unit test cases written FIRST (RED) and listed.
- [ ] Production code written to pass tests (GREEN).
- [ ] Code refactored (REFACTOR).
- [ ] Integration tests pass against Keycloak Testcontainer.
- [ ] Login flow works end-to-end.
- [ ] Callback validates state, nonce, and PKCE.
- [ ] Tokens stored server-side only.
- [ ] Cookie has HttpOnly, Secure, SameSite attributes.
- [ ] CSRF protection works on state-changing endpoints.
- [ ] Refresh Token rotation works.
- [ ] Logout invalidates session and calls Keycloak.
- [ ] Keycloak unavailability returns 503 (not 500).
- [ ] No tokens or secrets appear in logs.
- [ ] Configuration files are complete and runnable.
- [ ] LDAP User Federation configured for Enterprise users (read-only passwords).
- [ ] Self-registration configured with CUSTOMER default role.
- [ ] Admin Role Management API built using Client Credentials flow.
- [ ] Failure scenarios documented.
- [ ] Trade-offs explained.
