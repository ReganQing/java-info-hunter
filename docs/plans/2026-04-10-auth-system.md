# JWT Authentication & Authorization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add JWT-based authentication and role-based authorization to protect all API endpoints, with a frontend login page and route guards.

**Architecture:** Stateless JWT authentication using Spring Security 6 + JWT tokens. Backend issues access tokens (15min) and refresh tokens (7d). Frontend stores tokens in httpOnly cookies (preferred) or localStorage. Three roles: ADMIN (full access), USER (read + limited write), ANONYMOUS (read-only public data).

**Tech Stack:** Spring Security 6, JJWT (io.jsonwebtoken), bcrypt password hashing, React Context + protected routes

**Branch:** `feature/auth-system`

---

## Phase A: Backend - Database & Entity

### Task 1: Create Flyway migration for users table

**Files:**
- Create: `javainfohunter-ai-service/src/main/resources/db/migration/V2__add_users_table.sql`

**Step 1:** Write migration SQL

```sql
-- Users table for authentication
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Refresh tokens table for token rotation
CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_revoked      BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Insert default admin user (password: admin123, BCrypt encoded)
-- Generate hash at runtime, do NOT hardcode
```

**Step 2:** Commit
```bash
git commit -m "feat: add users and refresh_tokens table migration"
```

### Task 2: Create User entity and repository

**Files:**
- Create: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/entity/User.java`
- Create: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/entity/RefreshToken.java`
- Create: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/repository/UserRepository.java`
- Create: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/repository/RefreshTokenRepository.java`

**Step 1:** Create User entity with fields: id, username, email, passwordHash, role (enum: ADMIN, USER), isEnabled, lastLoginAt, createdAt, updatedAt. Use JPA annotations.

**Step 2:** Create RefreshToken entity with fields: id, userId, tokenHash, expiresAt, createdAt, isRevoked.

**Step 3:** Create UserRepository with methods: findByUsername, findByEmail, existsByUsername, existsByEmail.

**Step 4:** Create RefreshTokenRepository with methods: findByTokenHash, deleteByUserId, revokeAllByUserId.

**Step 5:** Commit
```bash
git commit -m "feat: add User and RefreshToken entities and repositories"
```

---

## Phase B: Backend - Spring Security & JWT

### Task 3: Add Spring Security and JJWT dependencies

**Files:**
- Modify: `javainfohunter-api/pom.xml`

**Step 1:** Add dependencies:

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Step 2:** Commit
```bash
git commit -m "feat: add Spring Security and JJWT dependencies"
```

### Task 4: Create JWT service

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/security/JwtService.java`

**Step 1:** Implement JwtService with:
- `generateAccessToken(User user)` → JWT with subject=username, claim=role, claim=userId, 15min expiry
- `generateRefreshToken(User user)` → random UUID-based token, store hash in DB, 7d expiry
- `validateAccessToken(String token)` → parse and verify signature/expiry
- `extractUsername(String token)` → get subject from JWT
- `extractRole(String token)` → get role claim
- Use `@Value("${javainfohunter.security.jwt.secret}")` for HMAC key
- Use `@Value("${javainfohunter.security.jwt.access-token-expiry:900000}")` for access token expiry

**Step 2:** Commit
```bash
git commit -m "feat: implement JWT service for token generation and validation"
```

### Task 5: Create JWT authentication filter

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/security/JwtAuthenticationFilter.java`

**Step 1:** Implement OncePerRequestFilter:
- Extract `Authorization: Bearer <token>` header
- Validate token via JwtService
- Load user from UserRepository
- Set `SecurityContextHolder` authentication with role-based `GrantedAuthority`
- Set `request.setAttribute("userId", user.getId())` for RateLimitAspect USER_ID key type

**Step 2:** Commit
```bash
git commit -m "feat: add JWT authentication filter"
```

### Task 6: Create Security configuration

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/security/SecurityConfig.java`

**Step 1:** Implement `SecurityFilterChain` bean:

```
PUBLIC (no auth required):
  - POST /api/v1/auth/login
  - POST /api/v1/auth/register
  - POST /api/v1/auth/refresh
  - GET  /api/v1/news/**         (read-only)
  - GET  /api/v1/test/**
  - GET  /actuator/health
  - Swagger/OpenAPI endpoints

USER role required:
  - GET  /api/v1/rss-sources
  - GET  /api/v1/rss-sources/{id}
  - GET  /api/v1/agents/**
  - GET  /api/v1/admin/status
  - GET  /api/v1/admin/resources
  - GET  /api/v1/admin/metrics

ADMIN role required:
  - POST /api/v1/rss-sources       (create)
  - PUT  /api/v1/rss-sources/{id}  (update)
  - DELETE /api/v1/rss-sources/{id} (delete)
  - POST /api/v1/rss-sources/{id}/crawl
  - POST /api/v1/admin/crawl/**
  - POST /api/v1/admin/crawl-by-category
```

**Step 2:** Disable CSRF (using JWT, not cookies), configure CORS, stateless session.
**Step 3:** Remove or reconcile the existing `CorsConfig.java` WebMvcConfigurer — Spring Security CORS takes precedence.

**Step 4:** Commit
```bash
git commit -m "feat: add Spring Security configuration with role-based endpoint protection"
```

### Task 7: Create Auth controller and service

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/controller/AuthController.java`
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/service/AuthService.java`
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/dto/request/LoginRequest.java`
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/dto/request/RegisterRequest.java`
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/dto/response/AuthResponse.java`

**Step 1:** Create DTOs:
- `LoginRequest`: username (or email), password
- `RegisterRequest`: username, email, password (min 8 chars)
- `AuthResponse`: accessToken, refreshToken, expiresIn, username, role

**Step 2:** Create AuthService:
- `login(LoginRequest)` → validate credentials with BCrypt, generate tokens, update lastLoginAt
- `register(RegisterRequest)` → check uniqueness, encode password, save User with USER role
- `refreshToken(String refreshToken)` → validate refresh token, issue new access token
- `logout(String refreshToken)` → revoke refresh token

**Step 3:** Create AuthController:
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

**Step 4:** Commit
```bash
git commit -m "feat: add authentication endpoints (login, register, refresh, logout)"
```

### Task 8: Add JWT configuration to application.yml

**Files:**
- Modify: `javainfohunter-api/src/main/resources/application.yml`
- Modify: `javainfohunter-api/src/main/resources/application-dev.yml`

**Step 1:** Add to application.yml:
```yaml
javainfohunter:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiry: ${JWT_ACCESS_EXPIRY:900000}   # 15 minutes
      refresh-token-expiry: ${JWT_REFRESH_EXPIRY:604800000} # 7 days
    initial-admin:
      username: ${ADMIN_USERNAME:admin}
      password: ${ADMIN_PASSWORD:}
      email: ${ADMIN_EMAIL:admin@javainfohunter.local}
```

**Step 2:** Add dev profile defaults with a dev-only secret.

**Step 3:** Commit
```bash
git commit -m "feat: add JWT security configuration to application.yml"
```

### Task 9: Data initializer for default admin

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/config/DataInitializer.java`

**Step 1:** Implement `CommandLineRunner` that:
- Checks if admin user exists by username
- If not, creates admin user with BCrypt-encoded password from env vars
- Logs the creation (not the password)

**Step 2:** Commit
```bash
git commit -m "feat: add DataInitializer for default admin user creation"
```

---

## Phase C: Frontend - Auth UI

### Task 10: Add auth API endpoints and types

**Files:**
- Modify: `src/shared/api/endpoints.ts` (frontend)
- Modify: `src/shared/api/types.ts`

**Step 1:** Add types:
```typescript
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  username: string;
  role: 'ADMIN' | 'USER';
}
export interface LoginRequest {
  username: string;
  password: string;
}
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
```

**Step 2:** Add authApi to endpoints.ts with login, register, refresh, logout.

**Step 3:** Add JWT interceptor to client.ts — add `Authorization: Bearer ${token}` header on every request.

**Step 4:** Commit
```bash
git commit -m "feat: add auth API endpoints and JWT interceptor to frontend"
```

### Task 11: Create auth context and store

**Files:**
- Create: `src/shared/stores/auth-store.ts` (frontend)
- Modify: `src/shared/api/client.ts` — add token refresh logic in error interceptor (401 → try refresh → retry)

**Step 1:** Create Zustand auth store with:
- `user`, `token`, `isAuthenticated`, `isAdmin`
- `login(username, password)`, `logout()`, `register(data)`, `refreshAuth()`
- Persist token to localStorage

**Step 2:** Add 401 response interceptor logic:
```
If 401 and has refresh token → call refresh endpoint → update token → retry original request
If refresh also fails → logout user, redirect to /login
```

**Step 3:** Commit
```bash
git commit -m "feat: add auth store with token refresh logic"
```

### Task 12: Create Login page

**Files:**
- Create: `src/modules/auth/login/index.tsx` (frontend)

**Step 1:** Create login page with:
- Username/password form with validation
- Error message display
- Link to register (if registration is open)
- Redirect to dashboard on success

**Step 2:** Commit
```bash
git commit -m "feat: add login page"
```

### Task 13: Add route guards to App.tsx

**Files:**
- Modify: `src/App.tsx` (frontend)

**Step 1:** Create `ProtectedRoute` component:
- Check `isAuthenticated` from auth store
- If not authenticated → redirect to `/login`
- If admin route and user is not admin → redirect to `/research/dashboard`

**Step 2:** Create `PublicOnlyRoute` component:
- If authenticated → redirect to `/research/dashboard`

**Step 3:** Update App.tsx routes:
- `/login` → PublicOnlyRoute → LoginPage
- `/admin/**` → ProtectedRoute (require ADMIN role)
- `/research/**` → accessible to all (public data)
- Add lazy-loaded Login page

**Step 4:** Commit
```bash
git commit -m "feat: add route guards and login page to App.tsx"
```

---

## Phase D: Testing & Polish

### Task 14: Write auth integration tests

**Files:**
- Create: `javainfohunter-api/src/test/java/com/ron/javainfohunter/api/controller/AuthControllerTest.java`

**Step 1:** Write tests:
- Register → 201 with tokens
- Register duplicate username → 409
- Login with correct credentials → 200 with tokens
- Login with wrong password → 401
- Access protected endpoint without token → 401
- Access protected endpoint with valid token → 200
- Access admin endpoint with USER role → 403
- Refresh token → 200 with new access token
- Logout → revoke refresh token

**Step 2:** Run tests: `mvnw.cmd test -pl javainfohunter-api`
**Step 3:** Commit
```bash
git commit -m "test: add auth controller integration tests"
```

### Task 15: Final verification and merge

**Step 1:** Run full backend build: `mvnw.cmd clean package`
**Step 2:** Run full frontend build: `npm run build`
**Step 3:** Merge branch to main
