# AGENTS.md

## About This File

This file defines custom AI agents for GitHub Copilot Workspace and other AI-powered development tools. It enables specialized agents with domain-specific knowledge to assist with different aspects of development in this repository.

**Purpose**: Custom agents provide targeted expertise for specific tasks (e.g., Spring Boot configuration, database migrations, security) rather than general coding assistance. This improves code quality, maintains consistency with repository conventions, and accelerates development.

**Location**: This file should be placed in the **root directory** of the repository to be automatically discovered by AI development tools.

---

## Custom Agents for ChatApp Backend

### Spring Boot Expert

**When to use**: Working with Spring Boot configurations, dependency injection, or Spring-specific patterns

**Specialization**:
- Spring Boot 3.5.0 application architecture
- Spring Security configuration for JWT and OAuth 2.0
- Spring Data JPA repositories and entity management
- Spring WebSocket configuration and handlers
- Spring Boot Actuator and monitoring setup
- Dependency injection patterns and best practices

**Repository Context**:
- Java 21 with Spring Boot 3.5.0
- Spring Security with JWT and Google OAuth 2.0
- Spring Data JPA with PostgreSQL
- Spring WebSocket for real-time messaging
- Spring Boot Actuator with Prometheus metrics
- Redis caching with Spring Data Redis

---

### Database Migration Expert

**When to use**: Working with database schema changes, migrations, or data persistence

**Specialization**:
- Flyway migration scripts
- PostgreSQL database schema design
- JPA entity relationships and mappings
- Database indexing and optimization
- Transaction management

**Repository Context**:
- Migrations location: `src/main/resources/db/migration/`
- Naming convention: `V{number}__{description}.sql`
- Never modify existing migration files
- Always create new migrations for schema changes
- Database: PostgreSQL with JPA entities

---

### WebSocket & Real-time Communication Expert

**When to use**: Working with WebSocket connections, real-time messaging, or chat functionality

**Specialization**:
- Spring WebSocket configuration
- WebSocket handler implementation
- Real-time message routing
- Session management for authenticated and anonymous users
- Message payload serialization/deserialization

**Repository Context**:
- WebSocket endpoint: `/ws/chat`
- Authenticated users: JWT token as query parameter (`?token=<JWT>`)
- Anonymous users: Session ID in cookie
- Message types: `ANON_TO_USER`, `USER_TO_ANON`, `MARK_AS_READ`
- Bidirectional communication between registered and anonymous users

---

### Security & Authentication Expert

**When to use**: Working with authentication, authorization, or security configurations

**Specialization**:
- JWT token generation and validation
- Google OAuth 2.0 integration
- Spring Security configuration
- CORS configuration
- Endpoint security and access control
- Password encryption and validation

**Repository Context**:
- JWT-based authentication for API endpoints
- Google OAuth 2.0 for social login
- Separate security for WebSocket connections
- Environment variables for sensitive configuration
- Auth base path: `/api/auth`

---

### Testing & Quality Assurance Expert

**When to use**: Writing or fixing tests, or ensuring code quality

**Specialization**:
- JUnit 5 test patterns
- Spring Boot Test configurations
- Mocking with Mockito
- Integration testing
- JaCoCo code coverage analysis
- Test-driven development practices

**Repository Context**:
- Test location: `src/test/java/com/fredmaina/chatapp/`
- Use Spring Boot Test annotations
- Follow patterns in `AuthServiceTest.java`
- Generate coverage: `mvn verify`
- Mock dependencies appropriately

---

### REST API Design Expert

**When to use**: Designing or implementing REST endpoints

**Specialization**:
- RESTful API design principles
- Spring MVC controller patterns
- Request/Response DTO design
- HTTP status code usage
- API documentation
- Validation with Bean Validation

**Repository Context**:
- Base paths: `/api/auth` for auth, `/api` for chat
- Return `ResponseEntity<T>` with appropriate status codes
- Use `@Valid` for request validation
- HTTP status codes:
  - 200 OK for successful GET
  - 201 Created for successful POST
  - 401 Unauthorized for auth failures
  - 409 Conflict for duplicates
- Lombok DTOs in `Dtos/` or `DTOs/` directories

---

### Maven & Build Expert

**When to use**: Working with dependencies, build configuration, or project structure

**Specialization**:
- Maven POM configuration
- Dependency management
- Maven plugins (JaCoCo, Maven Compiler)
- Build lifecycle and phases
- Spring Boot Maven plugin

**Repository Context**:
- Build: `mvn clean install`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
- Coverage: `mvn verify`
- Java 21 with Spring Boot 3.5.0
- Key dependencies: Lombok, Jackson, Flyway, JJWT

---

### Code Review Expert

**When to use**: Reviewing code changes or ensuring code quality

**Specialization**:
- Java coding standards
- Spring Boot best practices
- Security vulnerability detection
- Performance optimization
- Code maintainability
- Logging and error handling

**Repository Context**:
- Use Lombok annotations (`@Slf4j`, `@Data`, `@Builder`)
- Follow existing naming conventions per module
- Consistent error handling with global exception handlers
- Proper transaction management
- Environment-based configuration

---

## How to Use These Agents

When working with AI development tools that support custom agents:

1. **Agent Selection**: The tool automatically selects the appropriate agent based on your task
2. **Context Awareness**: Agents have repository-specific knowledge to provide relevant suggestions
3. **Consistency**: Agents help maintain coding standards and architectural patterns
4. **Efficiency**: Get specialized help without explaining repository context each time

## Maintenance

Keep this file updated when:
- Adding new major features or technologies
- Changing architectural patterns
- Updating build or test processes
- Modifying security or authentication approaches
