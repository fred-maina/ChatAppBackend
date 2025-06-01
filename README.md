# ChatApp - Real-time Messaging Application





## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
  - [Authentication](#authentication)
  - [Chat](#chat)
  - [WebSocket](#websocket)
- [Usage](#usage)
- [Database Migrations](#database-migrations)

---

## Overview

ChatApp is a robust backend system for a real-time messaging platform. It allows registered users to receive and respond to messages from anonymous users. The application handles user authentication, including social login with Google, manages chat sessions, and persists message history. Communication is powered by WebSockets for a seamless, real-time experience.

This project serves as a strong foundation for a customer support chat, an anonymous feedback system, or any application requiring interaction between authenticated users and anonymous guests.

---

## Features

* **User Authentication:**
    * Secure Email/Password Registration & Login.
    * Google OAuth 2.0 Integration for social login.
    * JWT (JSON Web Tokens) for stateless and secure API authentication.
    * Endpoint to check for username availability.
    * Ability for Google OAuth users to set a unique username post-registration.
* **Real-time Chat:**
    * WebSocket-based bi-directional communication for instant messaging.
    * Anonymous users can initiate chats with registered users using a session ID.
    * Registered users can receive messages from and reply to anonymous users.
    * Messages are tagged with sender type (`self` or `anonymous`), timestamp, and content.
* **Chat Management (for registered users):**
    * View a list of active and past chat sessions with anonymous users.
    * Retrieve detailed chat history for each session.
    * Messages from anonymous users can be marked as read.
    * Ability to delete entire chat sessions.
* **Database & Persistence:**
    * Uses PostgreSQL for relational data storage.
    * Database schema and evolutions managed with Flyway migrations.
* **Backend Architecture:**
    * Built with Java and the Spring Boot framework (including Spring Web, Spring Security, Spring Data JPA).
    * Well-defined RESTful APIs for authentication, user management, and chat operations.
    * Secure by default with Spring Security, including CORS configuration.

---

## Technologies Used

* **Backend:**
    * Java 21
    * Spring Boot
        * Spring Web (for REST APIs)
        * Spring Security (for authentication and authorization)
        * Spring Data JPA (for database interaction)
        * Spring WebSocket (for real-time communication)
* **Real-time Communication:**
    * Java WebSockets API
    * SockJS (client-side fallback)
    * STOMP (messaging protocol over WebSocket)
* **Database:**
    * PostgreSQL
* **Authentication & Authorization:**
    * JSON Web Tokens (JWT)
    * Google OAuth 2.0
* **Database Migrations:**
    * Flyway
* **Libraries & Tools:**
    * Lombok
    * Jackson
    * Maven


---

<details> <summary><strong>📁 Project Structure</strong></summary>

chatapp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fredmaina/chatapp/
│   │   │       ├── ChatappApplication.java      # Main application entry point
│   │   │       ├── Auth/                        # Authentication module
│   │   │       │   ├── configs/                 # Security, JWT, OAuth configs
│   │   │       │   ├── controllers/             # Auth REST controllers
│   │   │       │   ├── Dtos/                    # Data Transfer Objects for auth
│   │   │       │   ├── Models/                  # User, Role entities
│   │   │       │   ├── Repositories/            # JPA repositories for auth
│   │   │       │   └── services/                # Auth business logic
│   │   │       └── core/                        # Core chat functionality
│   │   │           ├── config/                  # WebSocket, Jackson configs
│   │   │           ├── Controllers/             # Chat REST & WebSocket controllers
│   │   │           ├── DTOs/                    # DTOs for chat messages, sessions
│   │   │           ├── models/                  # ChatMessage entity
│   │   │           ├── Repositories/            # JPA repositories for chat
│   │   │           └── Services/                # Chat business logic
│   ├── resources/
│   │   ├── application.properties               # Application configuration
│   │   ├── db/migration/                        # Flyway SQL migration scripts
│   │   └── static/
│   │       └── index.html                       # Basic HTML for WebSocket testing
├── test/
│   └── java/
└── pom.xml
</details>
---

## Prerequisites

* Java Development Kit (JDK) 21 or higher.
* Apache Maven or Gradle (specify which one is used and its version).
* PostgreSQL server running.
* Google Cloud Platform project with OAuth 2.0 credentials configured (Client ID & Client Secret).

---

## Setup and Installation

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd chatapp
    ```

2.  **Database Setup:**
    * Ensure your PostgreSQL server is running.
    * Create a new database for the application (e.g., `chatapp_db`).
    * Flyway migrations will automatically create the necessary tables.

3.  **Configure Application Properties:**
    * Update `src/main/resources/application.properties` (or use environment variables) with your specific configurations. See the [Configuration](#configuration) section.

4.  **Build the project:**
    * If using Maven:
        ```bash
        mvn clean install
        ```


---

## Configuration

The primary configuration is in `src/main/resources/application.properties`. Set these properties, preferably using environment variables:

* **Database Connection:**
    * `SPRING_DATASOURCE_URL`
    * `SPRING_DATASOURCE_USERNAME`
    * `SPRING_DATASOURCE_PASSWORD`

* **JWT Configuration:**
    * `JWT_SECRET`
    * `jwt.expiration`

* **Google OAuth 2.0:**
    * `GOOGLE_CLIENT-ID`
    * `GOOGLE_SECRET-ID`
    * `GOOGLE_REDIRECT_URI`

* **Security CORS Configuration:**
    * `security.allowed-origins`
    * `security.allowed-methods`
    * `security.allowed-headers`
    * `security.public-endpoints`

---

## Running the Application

* If using Maven:
    ```bash
    mvn spring-boot:run
    ```
The application typically starts on `http://localhost:8080`.

---

## API Endpoints

### Authentication
Base Path: `/api/auth`

* `POST /register`: User registration.
* `POST /login`: User login.
* `POST /oauth/google`: Google OAuth flow.
* `GET /me`: Current authenticated user details. (Requires `Authorization: Bearer <JWT_TOKEN>`)
* `POST /set-username`: Set username (e.g., after Google OAuth).
* `GET /check-username/{username}`: Check username availability.

### Chat
Base Path: `/api`

* `GET /chats`: Authenticated user's chat sessions. (Requires `Authorization: Bearer <JWT_TOKEN>`)
* `GET /chat/session_history`: Chat history for an anonymous session. (Query Params: `sessionId`, `recipient`)
* `DELETE /chat/{anonSessionId}`: Delete a chat session. (Requires `Authorization: Bearer <JWT_TOKEN>`)

### WebSocket
* **Endpoint:** `/ws/chat`
* **Connection:**
    * **Authenticated Users:** `ws://localhost:8080/ws/chat?token=<YOUR_JWT_TOKEN>`
    * **Anonymous Users:** Connect with `anonSessionId` in a cookie.
* **Message Payload:** `WebSocketMessagePayload` JSON
    ```json
    {
      "type": "ANON_TO_USER" | "USER_TO_ANON" | "MARK_AS_READ",
      "from": "username_or_sessionId",
      "to": "username_or_sessionId",
      "content": "Your message content",
      "nickname": "AnonymousUserNickname", // Optional
      "timestamp": "ISO_DATE_TIME_STRING"
    }
    ```

---

## Usage

1.  **Anonymous User:**
    * Frontend generates a unique `anonSessionId`.
    * Connect to `/ws/chat` with `anonSessionId` (e.g., via cookie).
    * Send messages with `type: "ANON_TO_USER"`.

2.  **Registered User:**
    * Register/Login to get a JWT.
    * Connect to `/ws/chat?token=<JWT>`.
    * Fetch sessions via `GET /api/chats`.
    * Reply via WebSocket with `type: "USER_TO_ANON"`.
    * View history/mark read via `GET /api/chat/session_history`.


## Database Migrations

Managed by Flyway. Scripts are in `src/main/resources/db/migration`.

