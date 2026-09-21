# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Task-management REST API: Spring Boot 4.1.1 / Java 21 / Maven (`com.agatha.todo_api`). Layered (controller → service → repository → entity), JWT authentication (jjwt 0.12.6), Spring Data JPA on in-memory H2 (no datasource config; data is lost on restart).

## Commands

Use the Maven wrapper (PowerShell: `.\mvnw.cmd`; Bash: `./mvnw`).

- Run the app: `./mvnw spring-boot:run`
- Build: `./mvnw clean package`
- All tests: `./mvnw test`
- Single test class: `./mvnw test -Dtest=AuthFlowTests`
- Single test method: `./mvnw test -Dtest=AuthFlowTests#usersOnlySeeTheirOwnTasks`

No linter is configured.

## Architecture

**Auth flow** (spans `security/`, `service/`, `controller/`):
- `POST /auth/register` and `POST /auth/login` are public (`AuthController` → `AuthService`). Passwords are BCrypt-hashed; login checks them directly with `PasswordEncoder` (no `AuthenticationManager`/`UserDetailsService` is used), then `JwtService` issues an HS256 token whose subject is the username.
- `JwtAuthenticationFilter` (registered before `UsernamePasswordAuthenticationFilter` in `SecurityConfig`) reads `Authorization: Bearer <token>`, validates it via `JwtService.extractUsername`, checks the user still exists, and sets the `SecurityContext` with the username as principal and no authorities.
- `SecurityConfig`: stateless, CSRF disabled, only `/auth/**` and `/error` are open, unauthenticated requests get a bare 401. Spring Boot's default in-memory user (generated password in the logs) is still auto-configured but unused.

**Per-user isolation**: `Task` has a `@ManyToOne` owner. `TaskService` derives the current user from `SecurityContextHolder` (never from request bodies) and every read/update/delete goes through `TaskRepository.findByIdAndOwner`/`findAllByOwner`. Another user's task yields 404, not 403, so its existence isn't leaked. Keep this rule for any new task endpoints.

**Errors**: no global `@RestControllerAdvice`; services throw `ResponseStatusException` (401/404/409) and DTO validation (`@Valid`, records in `dto/`) falls back to Spring's default 400. Entities are never returned directly; use the DTOs.

**Config**: JWT settings live under `app.jwt` in `application.yaml`. `secret` is Base64 (≥32 bytes) read from the `JWT_SECRET` env var, with a dev-only default; `expiration-ms` defaults to 1h.

## Testing notes

`AuthFlowTests` is a `@SpringBootTest` + MockMvc integration test of the whole flow (Spring Boot 4 import: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`). It parses JSON with regex because Jackson 3 ships with Boot 4 while `jjwt-jackson` uses Jackson 2; both coexist without issues so far.