# Custom Agents for ChatApp Backend

This file defines custom AI agents that can assist with specific tasks in this repository.

## Spring Boot Expert

**Trigger**: When working with Spring Boot configurations, dependency injection, or Spring-specific patterns

**Expertise**:
- Spring Boot 3.5.0 application architecture
- Spring Security configuration for JWT and OAuth 2.0
- Spring Data JPA repositories and entity management
- Spring WebSocket configuration and handlers
- Spring Boot Actuator and monitoring setup
- Dependency injection patterns and best practices

**Context**:
This is a Spring Boot application using:
- Java 21
- Spring Security with JWT and Google OAuth 2.0
- Spring Data JPA with PostgreSQL
- Spring WebSocket for real-time messaging
- Spring Boot Actuator with Prometheus metrics
- Redis caching with Spring Data Redis

## Database Migration Expert

**Trigger**: When working with database schema changes, migrations, or data persistence

**Expertise**:
- Flyway migration scripts
- PostgreSQL database schema design
- JPA entity relationships and mappings
- Database indexing and optimization
- Transaction management

**Context**:
- Database migrations are in `src/main/resources/db/migration/`
- Naming convention: `V{number}__{description}.sql`
- Never modify existing migration files
- Always create new migrations for schema changes
- Database: PostgreSQL with JPA entities

## WebSocket & Real-time Communication Expert

**Trigger**: When working with WebSocket connections, real-time messaging, or chat functionality

**Expertise**:
- Spring WebSocket configuration
- WebSocket handler implementation
- Real-time message routing
- Session management for authenticated and anonymous users
- Message payload serialization/deserialization

**Context**:
- WebSocket endpoint: `/ws/chat`
- Authenticated users: JWT token as query parameter
- Anonymous users: Session ID in cookie
- Message types: `ANON_TO_USER`, `USER_TO_ANON`, `MARK_AS_READ`
- Bidirectional communication between registered and anonymous users

## Security & Authentication Expert

**Trigger**: When working with authentication, authorization, or security configurations

**Expertise**:
- JWT token generation and validation
- Google OAuth 2.0 integration
- Spring Security configuration
- CORS configuration
- Endpoint security and access control
- Password encryption and validation

**Context**:
- JWT-based authentication for API endpoints
- Google OAuth 2.0 for social login
- Separate security for WebSocket connections
- Environment variables for sensitive configuration
- Base path: `/api/auth` for authentication endpoints

## Testing & Quality Assurance Expert

**Trigger**: When writing or fixing tests, or ensuring code quality

**Expertise**:
- JUnit 5 test patterns
- Spring Boot Test configurations
- Mocking with Mockito
- Integration testing
- JaCoCo code coverage analysis
- Test-driven development practices

**Context**:
- Tests in: `src/test/java/com/fredmaina/chatapp/`
- Use Spring Boot Test annotations
- Follow existing test patterns in `AuthServiceTest.java`
- Coverage reports generated with: `mvn verify`
- Mock dependencies appropriately

## REST API Design Expert

**Trigger**: When designing or implementing REST endpoints

**Expertise**:
- RESTful API design principles
- Spring MVC controller patterns
- Request/Response DTO design
- HTTP status code usage
- API documentation
- Validation with Bean Validation

**Context**:
- Base paths: `/api/auth` for auth, `/api` for chat
- Return `ResponseEntity<T>` with appropriate status codes
- Use `@Valid` for request validation
- Standard status codes:
  - 200 OK for successful GET
  - 201 Created for successful POST
  - 401 Unauthorized for auth failures
  - 409 Conflict for duplicates
- Lombok DTOs in `Dtos/` or `DTOs/` directories

## Maven & Build Expert

**Trigger**: When working with dependencies, build configuration, or project structure

**Expertise**:
- Maven POM configuration
- Dependency management
- Maven plugins (JaCoCo, Maven Compiler)
- Build lifecycle and phases
- Spring Boot Maven plugin

**Context**:
- Build: `mvn clean install`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
- Coverage: `mvn verify`
- Java 21 with Spring Boot 3.5.0
- Key dependencies: Lombok, Jackson, Flyway, JJWT

## Code Review Expert

**Trigger**: When reviewing code changes or ensuring code quality

**Expertise**:
- Java coding standards
- Spring Boot best practices
- Security vulnerability detection
- Performance optimization
- Code maintainability
- Logging and error handling

**Context**:
- Use Lombok annotations (`@Slf4j`, `@Data`, `@Builder`)
- Follow existing naming conventions per module
- Consistent error handling with global exception handlers
- Proper transaction management
- Environment-based configuration
