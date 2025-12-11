# AGENTS.md

A guide for AI coding agents working on this repository.

## Setup commands
- Install dependencies: `mvn clean install`
- Start dev server: `mvn spring-boot:run`
- Run tests: `mvn test`
- Generate coverage report: `mvn verify`

## Tech stack
- Java 21
- Spring Boot 3.5.0
- PostgreSQL with Flyway migrations
- Spring Security with JWT and OAuth 2.0
- Spring WebSocket for real-time messaging
- Redis for caching
- Maven for build management

## Code style
- Use Lombok annotations (`@Slf4j`, `@Data`, `@Builder`) to reduce boilerplate
- Follow Spring Boot best practices and conventions
- Use constructor injection for dependencies
- Use `@Valid` annotation for request body validation
- Log important operations using SLF4J

## Project structure
- `src/main/java/com/fredmaina/chatapp/Auth/` - Authentication module (lowercase `controllers/`, `services/` but capitalized `Dtos/`, `Models/`, `Repositories/`)
- `src/main/java/com/fredmaina/chatapp/core/` - Core chat functionality (capitalized `Controllers/`, `Services/`, `DTOs/`)
- `src/main/resources/db/migration/` - Flyway database migrations
- `src/test/java/` - Test files

## Database migrations
- Location: `src/main/resources/db/migration/`
- Naming: `V{number}__{description}.sql` (e.g., `V1__create_users_table.sql`)
- Never modify existing migration files
- Always create new migrations for schema changes

## API conventions
- Auth endpoints: `/api/auth`
- Chat endpoints: `/api`
- Return `ResponseEntity<T>` with appropriate HTTP status codes
- Use standard status codes: 200 OK, 201 Created, 401 Unauthorized, 409 Conflict

## WebSocket
- Endpoint: `/ws/chat`
- Authenticated users: JWT token as query parameter (`?token=<JWT>`)
- Anonymous users: Session ID in cookie
- Message types: `ANON_TO_USER`, `USER_TO_ANON`, `MARK_AS_READ`

## Security
- JWT authentication for API endpoints
- Google OAuth 2.0 for social login
- Store sensitive config in environment variables (`JWT_SECRET`, `GOOGLE_CLIENT_ID`, etc.)
- Always validate and sanitize user input

## Testing
- Tests in: `src/test/java/com/fredmaina/chatapp/`
- Use JUnit 5 and Spring Boot Test annotations
- Mock dependencies with Mockito
- Follow existing patterns in `AuthServiceTest.java`
