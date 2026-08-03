# IAM AND BFF RULES

These rules govern Identity and Access Management, the BFF (Backend for Frontend) service, authentication flows, token handling, and authorization across the entire system.

---

## 1. Keycloak as Identity Provider

Keycloak is the sole Identity and Access Management system. No business service may implement its own username/password authentication.

Keycloak responsibilities:
- User registration and login.
- Logout (single and global).
- User management.
- Credential management.
- Password policy enforcement.
- Email verification (when configured).
- Password reset.
- Multi-Factor Authentication (future expansion).
- OpenID Connect (OIDC) protocol.
- OAuth 2.0 protocol.
- Access Token, Refresh Token, ID Token issuance.
- Realm roles, Client roles, Groups.

---

## 2. BFF Service

A dedicated `bff-service` MUST exist between the browser and the backend system. The BFF is the only service that the browser communicates with directly (aside from static assets).

### BFF Responsibilities

- Initiate Authorization Code Flow with PKCE.
- Redirect user to Keycloak login page.
- Receive authorization code at callback endpoint.
- Exchange authorization code for tokens from Keycloak token endpoint.
- Store Access Token and Refresh Token server-side (in a secure session store).
- NEVER return Refresh Token to JavaScript.
- Set session cookie for the browser.
- Refresh Access Token before expiration.
- Logout from both internal session and Keycloak.
- CSRF protection for all state-changing requests.
- Call downstream services on behalf of the user.
- Forward user identity and authorization context securely.
- Aggregate API responses when UI needs data from multiple services.

### BFF Does NOT Contain

- Core business logic (ticket reservation, inventory management, payment processing).
- Direct database access to business data.

---

## 3. Cookie Security

Session cookie set by BFF MUST have:

| Attribute | Requirement |
|---|---|
| `HttpOnly` | Always true — JavaScript must not access the cookie |
| `Secure` | True in production (HTTPS only) |
| `SameSite` | `Lax` or `Strict` depending on cross-origin requirements |
| `Path` | Scoped appropriately |
| `Domain` | Scoped appropriately |
| `Max-Age` | Aligned with session duration |

---

## 4. Token Storage Prohibitions

Access Token and Refresh Token MUST NOT be stored in:
- localStorage.
- sessionStorage.
- Long-lived JavaScript variables.
- Unencrypted cookies.
- Cookies without `HttpOnly` flag.

---

## 5. Authentication Flow

### Login Flow

```
Browser
  │ GET /api/auth/login
  ▼
BFF Service
  │ Generate state + nonce + PKCE code_verifier/code_challenge
  │ Store state, nonce, code_verifier in server-side session
  │ Redirect to Keycloak /auth?response_type=code&client_id=...&code_challenge=...&state=...&nonce=...
  ▼
Keycloak
  │ User authenticates
  │ Redirect to BFF callback with authorization_code + state
  ▼
BFF Callback Endpoint
  │ Validate state matches session
  │ Exchange authorization_code + code_verifier for tokens at Keycloak token endpoint
  ▼
Keycloak Token Endpoint
  │ Returns Access Token + Refresh Token + ID Token
  ▼
BFF Session Store
  │ Validate nonce in ID Token
  │ Store tokens server-side
  │ Set HttpOnly session cookie for browser
  ▼
Browser
```

### Authenticated API Call Flow

```
Browser
  │ Session Cookie + CSRF Token
  ▼
BFF Service
  │ Resolve server-side session
  │ Validate CSRF token
  │ Check Access Token expiration (refresh if needed)
  │ Attach Bearer Access Token to outbound request
  ▼
API Gateway
  │ Validate JWT (signature, issuer, expiration)
  │ Add Correlation ID
  │ Route to appropriate service
  ▼
Business Service
  │ Validate JWT independently
  │ Check roles/scopes
  │ Check resource ownership
  │ Process request
```

### Logout Flow

```
Browser
  │ POST /api/auth/logout (with CSRF token)
  ▼
BFF Service
  │ Invalidate server-side session
  │ Call Keycloak end_session_endpoint with id_token_hint
  │ Clear session cookie
  ▼
Browser
```

---

## 6. Token Validation

API Gateway and every resource service MUST validate JWT using:
- **Issuer** (`iss` claim) — must match Keycloak realm URL.
- **Signature** — verified against Keycloak's JWKS endpoint.
- **Expiration** (`exp` claim).
- **Audience** (`aud` claim) — when the system uses audience-based access control.
- **Authorized party** (`azp` claim) — when needed.
- **Roles or scopes** — extracted from token claims.

Do NOT call Keycloak introspection endpoint for every request when the token is a valid self-contained JWT, unless there is a specific requirement (e.g., token revocation check).

### JWKS Requirements

- Cache public keys from JWKS endpoint.
- Support `kid` (Key ID) for key selection.
- Support key rotation — refresh JWKS when an unknown `kid` appears.
- Do NOT hardcode public keys.

---

## 7. Authorization

### Technology Stack

- Spring Security.
- OAuth2 Resource Server configuration.
- Method Security enabled.
- `@PreAuthorize` for endpoint-level access control.
- Scope-based and role-based authorization.
- Ownership checks in domain/application layer (not just annotation-based).

### Roles

| Role | Purpose |
|---|---|
| CUSTOMER | Browse events, reserve tickets, place orders, pay, view own tickets |
| EVENT_ORGANIZER | Create/manage events, shows, ticket types, venues |
| STAFF | Perform ticket check-in |
| ADMIN | System configuration, audit logs, reconciliation, user management |
| SUPPORT | Customer issue handling, manual refunds, order investigation |

### Scopes

| Scope | Purpose |
|---|---|
| event:read | Read event catalog |
| event:write | Create/update events |
| inventory:read | View inventory quantities |
| inventory:manage | Reserve, confirm, release inventory |
| order:read | View orders |
| order:create | Create orders |
| order:cancel | Cancel orders |
| payment:create | Initiate payments |
| payment:refund | Process refunds |
| ticket:read | View tickets |
| ticket:checkin | Check in tickets |

### Authorization Rules

- Do NOT rely solely on Gateway-level role checks. Every resource service MUST protect its own endpoints independently.
- Ownership authorization (e.g., "user can only view their own orders") MUST be enforced in the domain/application layer, not just at the API layer.
- Admin endpoints must require both role check AND audit logging.

---

## 8. Responsibility Distribution

```
┌─────────────────────────────────────────────────────────┐
│ BFF SERVICE                                             │
│ - Manage browser sessions                               │
│ - Hold tokens server-side                               │
│ - Login, callback, refresh, logout                      │
│ - CSRF protection                                       │
│ - Call backend on behalf of browser                     │
├─────────────────────────────────────────────────────────┤
│ API GATEWAY                                             │
│ - Routing                                               │
│ - Rate limiting                                         │
│ - Header sanitization (strip spoofed identity headers)  │
│ - Correlation ID injection                              │
│ - JWT validation at edge                                │
│ - Request size limits                                   │
│ - Timeout enforcement                                   │
├─────────────────────────────────────────────────────────┤
│ RESOURCE SERVICE                                        │
│ - Validate JWT independently (do not trust gateway)     │
│ - Check roles and scopes                                │
│ - Check resource ownership                              │
│ - Enforce business-level authorization                  │
│ - Audit sensitive actions                               │
└─────────────────────────────────────────────────────────┘
```

---

## 9. Keycloak Test Configuration

For automated testing, provide a separate test realm configuration:

- `realm-test.json` containing:
  - Test client for BFF (with redirect URIs for test environment).
  - Test client for API (resource server).
  - Test users with known credentials.
  - Test roles (CUSTOMER, EVENT_ORGANIZER, STAFF, ADMIN, SUPPORT).
  - Appropriate token lifetimes for testing.

- Do NOT use production realm configuration for automated tests.
- Do NOT use real user credentials in test configuration.
- Keycloak Testcontainer MUST be used for BFF and security integration tests.
