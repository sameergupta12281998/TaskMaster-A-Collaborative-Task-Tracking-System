# 📌 TaskMaster — Collaborative Task Tracking System

A production-grade, industrial-ready RESTful backend built with **Java 17** and **Spring Boot 3.2** for collaborative task management. Supports full CRUD for tasks, teams, comments, and file attachments with JWT-based authentication, role-based authorization, pagination, filtering, and comprehensive logging.

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security + JWT (jjwt 0.12.5) |
| **Database** | PostgreSQL + Spring Data JPA / Hibernate |
| **Validation** | Jakarta Bean Validation |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Logging** | SLF4J + Logback (rolling file + error file) |
| **Build** | Maven |
| **Testing** | JUnit 5 + Mockito + MockMvc + H2 |

---

## 📂 Project Structure

```
src/main/java/com/taskmaster/
├── config/             # Security, OpenAPI, CORS configuration
├── controller/         # REST controllers (Auth, User, Task, Team, Comment, Attachment)
├── dto/
│   ├── request/        # Validated request DTOs
│   └── response/       # Response DTOs & API wrapper
├── entity/             # JPA entities (User, Task, Team, Comment, Attachment, RefreshToken)
│   └── enums/          # TaskStatus, TaskPriority, TeamRole
├── exception/          # Custom exceptions + GlobalExceptionHandler
├── mapper/             # Entity ↔ DTO mappers
├── repository/         # Spring Data JPA repositories + Specifications
│   └── specification/  # JPA Specification builders
├── security/           # JWT provider, filter, entry point, UserDetailsService
├── service/            # Service interfaces
│   └── impl/           # Service implementations
└── util/               # Constants
```

---

## 🔌 API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT tokens |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Revoke refresh token |

### Users (`/api/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/update` | Update current user profile |

### Tasks (`/api/tasks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create a new task |
| GET | `/api/tasks` | List tasks with filtering, search, sort, pagination |
| GET | `/api/tasks/{id}` | Get task details |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| GET | `/api/tasks/my-tasks` | Get tasks assigned to/created by current user |

### Teams (`/api/teams`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/teams` | Create a new team |
| GET | `/api/teams/{id}` | Get team details |
| POST | `/api/teams/{id}/join` | Join a team |
| POST | `/api/teams/{id}/members` | Add a member to team |
| DELETE | `/api/teams/{id}/members/{memberId}` | Remove a member |

### Comments (`/api/tasks/{taskId}/comments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks/{taskId}/comments` | Add comment to task |
| GET | `/api/tasks/{taskId}/comments` | Get comments for task |

### Attachments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks/{taskId}/attachments` | Upload file to task |
| GET | `/api/tasks/{taskId}/attachments` | List task attachments |
| GET | `/api/attachments/{id}/download` | Download attachment |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 14+**

### 1. Database Setup

```sql
CREATE DATABASE taskmaster;
CREATE USER taskmaster_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE taskmaster TO taskmaster_user;
```

### 2. Configure Environment

Set environment variables or update `application.yml`:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/taskmaster
export DB_USERNAME=taskmaster_user
export DB_PASSWORD=your_password
export JWT_SECRET=your_base64_encoded_secret_key_at_least_256_bits
export CORS_ORIGINS=http://localhost:3000
```

### 3. Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/taskmaster-1.0.0.jar

# OR run with Maven
mvn spring-boot:run
```

### 4. Access

- **API**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/api-docs`
- **Health Check**: `http://localhost:8080/actuator/health`

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TaskServiceTest

# Run with coverage
mvn test jacoco:report
```

---

## 🔒 Security Features

- **BCrypt** password hashing (strength 12)
- **JWT** access tokens (configurable expiration, default 15 min)
- **Refresh token rotation** with revocation
- **Path traversal protection** on file uploads
- **File extension whitelisting**
- **Input validation** on all endpoints
- **CORS** configuration
- **Stateless** session management
- Global exception handling (no stack traces leaked to clients)

---

## 📊 Logging

- **Console + Rolling File** appenders
- **Separate error log** (`logs/taskmaster-error.log`)
- **30-day retention** with gzip compression
- **Structured log pattern**: `timestamp [thread] [traceId] level logger - message`
- Per-profile log levels (DEBUG in dev, INFO in prod)

---

## ⚙️ Configuration

All configuration is externalized via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskmaster` | Database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | (dev default) | JWT signing key (Base64) |
| `JWT_ACCESS_EXPIRATION` | `900000` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token TTL (ms) |
| `SERVER_PORT` | `8080` | Server port |
| `UPLOAD_DIR` | `./uploads` | File upload directory |
| `MAX_FILE_SIZE` | `10MB` | Max upload file size |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed CORS origins |
| `LOG_LEVEL` | `INFO` | Application log level |
TaskMaster: Collaborative Task Tracking System.



#updated PR
