# Copilot Instructions for ChatApp Backend

## Project Overview

ChatApp is a real-time messaging platform backend built with Java 21 and Spring Boot. It enables registered users to receive and respond to messages from anonymous users using WebSocket technology for real-time communication.

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.0
- **Build Tool**: Maven
- **Database**: PostgreSQL with Flyway migrations
- **Real-time**: Spring WebSocket
- **Security**: Spring Security with JWT and OAuth 2.0 (Google)
- **ORM**: Spring Data JPA
- **Utilities**: Lombok, Jackson
- **Monitoring**: Spring Boot Actuator with Prometheus metrics
- **Caching**: Redis (Spring Data Redis)

## Build and Test Commands

### Build
```bash
mvn clean install
```

### Run Application
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

### Generate Coverage Report
```bash
mvn verify
```

The application runs on `http://localhost:8080` by default.

## Project Structure

```
src/main/java/com/fredmaina/chatapp/
├── ChatappApplication.java       # Main application entry point with @EnableCaching
├── Auth/                          # Authentication and authorization module
│   ├── controllers/              # REST controllers for auth endpoints
│   ├── Dtos/                     # Data Transfer Objects (note capitalization)
│   ├── Models/                   # Entity models
│   ├── Repositories/             # JPA repositories
│   ├── services/                 # Business logic (JWT, Auth)
│   ├── configs/                  # Security configurations
│   └── exceptions/               # Exception handling
└── core/                         # Core chat functionality
    ├── Controllers/              # Chat REST controllers
    ├── DTOs/                     # Data Transfer Objects
    ├── models/                   # Chat entity models
    ├── Repositories/             # Chat repositories
    ├── Services/                 # Chat and messaging services
    └── config/                   # WebSocket and other configs
```

## Coding Conventions

### General Practices
- Use **Lombok** annotations to reduce boilerplate (`@Slf4j`, `@Data`, `@Builder`, etc.)
- Follow Spring Boot best practices and conventions
- Use constructor injection with `@Autowired` (or prefer constructor injection without @Autowired)
- Use `@Valid` annotation for request body validation
- Log important operations using SLF4J (`@Slf4j`)

### Naming Conventions
- **Controllers**: Suffix with `Controller` (e.g., `AuthController`, `ChatController`)
- **Services**: Suffix with `Service` (e.g., `AuthService`, `ChatService`)
- **DTOs**: Place in `Dtos/` or `DTOs/` directories (note mixed case in existing code)
- **Repositories**: Suffix with `Repository` and extend Spring Data JPA interfaces
- **Models/Entities**: Use JPA annotations (`@Entity`, `@Table`, etc.)

### Package Structure
- **Auth module**: Uses lowercase `controllers`, `services`, but capitalized `Dtos`, `Models`, `Repositories`
- **Core module**: Uses capitalized `Controllers`, `Services`, `DTOs`, etc.
- Maintain consistency within each module when adding new files

### REST API Conventions
- Base paths: `/api/auth` for authentication, `/api` for chat operations
- Return `ResponseEntity<T>` with appropriate HTTP status codes
- Use standard HTTP status codes:
  - 200 OK for successful GET
  - 201 Created for successful POST (registration)
  - 401 Unauthorized for authentication failures
  - 409 Conflict for duplicate resources
- Use `@RequestBody` for request payloads
- Use `@PathVariable` and `@RequestParam` for URL parameters

### Security
- **JWT Authentication**: Use for API authentication
- **Google OAuth 2.0**: Supported for social login
- **WebSocket Security**: Token-based for authenticated users, session-based for anonymous
- Always validate and sanitize user input
- Use Spring Security for endpoint protection
- Store sensitive configuration in environment variables

## Database

### Flyway Migrations
- Located in: `src/main/resources/db/migration/`
- Naming: `V{number}__{description}.sql` (e.g., `V1__create_users_table.sql`)
- Always create migrations for schema changes
- Never modify existing migration files

### Configuration
Use environment variables for database connection:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## WebSocket

- **Endpoint**: `/ws/chat`
- **Authenticated users**: Connect with JWT token as query parameter (`?token=<JWT>`)
- **Anonymous users**: Connect with `anonSessionId` in cookie
- Message types: `ANON_TO_USER`, `USER_TO_ANON`, `MARK_AS_READ`

## Testing

- Tests located in: `src/test/java/com/fredmaina/chatapp/`
- Use JUnit and Spring Boot Test annotations
- Mock dependencies appropriately
- Follow existing test patterns in `AuthServiceTest.java`
- Code coverage tracked with JaCoCo

## Environment Configuration

Configuration is in `src/main/resources/application.properties`. Use environment variables:

**Required**:
- `JWT_SECRET` - Secret key for JWT signing
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_SECRET_ID` - Google OAuth client secret
- `GOOGLE_REDIRECT_URI` - OAuth redirect URI

**Optional**:
- `security.allowed-origins` - CORS allowed origins
- `security.allowed-methods` - CORS allowed methods
- `security.allowed-headers` - CORS allowed headers

## Common Patterns

### Controller Pattern
```java
@Slf4j
@Controller
@RequestMapping("/api/...")
public class ExampleController {
    @Autowired
    private ExampleService service;
    
    @PostMapping("/endpoint")
    public ResponseEntity<ResponseDto> method(@Valid @RequestBody RequestDto request) {
        // Implementation
    }
}
```

### Service Pattern
- Business logic goes in services
- Services interact with repositories
- Use transactions where appropriate (`@Transactional`)

### Exception Handling
- Global exception handling is implemented
- Use custom exceptions when appropriate
- Return meaningful error messages in responses

## Additional Notes

- The application uses caching (Redis) - consider cache invalidation when modifying data
- Actuator endpoints available for monitoring
- Prometheus metrics exposed for observability
- Spring DevTools enabled for development
- Follow the contribution guidelines in `CONTRIBUTING.md`
