# Auth Microservice | EduLearn Identity Hub

The Auth Service is the security gateway and identity provider for the EduLearn platform. It manages user lifecycle, provides secure JWT issuance, and implements complex Role-Based Access Control (RBAC).

---

## 🏛️ Service Overview
This service handles all aspects of authentication, authorization, and profile management. It is designed to be the single source of truth for user identities across the entire microservices ecosystem.

- **Port**: 8081
- **Database**: `lms_auth` (MySQL 8.0)
- **Key Responsibility**: Issuing and validating bearer tokens for cross-service security.

---

## 🚀 Key Features

### 1. Unified Authentication Flow
- Supports standard Email/Password registration and login.
- **Social Login**: Fully integrated Google OAuth2 support for frictionless onboarding.
- **JWT Issuance**: Generates secure, signed JSON Web Tokens containing user roles and metadata.

### 2. Multi-Role Management
- **Student**: Access to learning catalogs, progress tracking, and discussions.
- **Instructor**: Permission to author courses, manage lessons, and view performance analytics.
- **Admin**: Global system control, user moderation, and high-level financial oversight.

### 3. Profile & Security
- Secure password hashing using BCrypt.
- Gravatar/Profile Picture URL management.
- Account recovery and password reset workflows.

### 4. System Governance
- **Bug Reporting**: Dedicated module for users to report platform issues directly to the administration.

---

## 💻 Frontend Integration (React Extras)
The frontend uses this service heavily for state management:
- **Redux Integration**: The `userSlice` in the React app persists JWT and profile data after a successful login.
- **Protected Routes**: Custom React components (`ProtectedRoute.jsx`) check user roles against the JWT stored in the browser state.
- **AI Mascot UI**: The animated AI mascot displayed on the landing page is dynamically customized based on the user's login status fetched from this service.
- **Glassmorphism Design**: The Login and Signup pages feature high-end glassmorphism effects, radial gradients, and responsive layouts to match the premium brand aesthetic.

---

## 📊 Database Schema (lms_auth)

### `users` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique user identifier. |
| `email` | VARCHAR (Unique) | Primary login credential. |
| `password` | VARCHAR | BCrypt hashed password. |
| `full_name` | VARCHAR | User's display name. |
| `role` | ENUM | STUDENT, INSTRUCTOR, ADMIN. |
| `profile_pic`| VARCHAR | S3 or Gravatar URL. |
| `created_at` | TIMESTAMP | Audit timestamp. |

### `bug_reports` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique report ID. |
| `user_id` | BIGINT (FK) | Reference to the reporter. |
| `title` | VARCHAR | Brief summary of the bug. |
| `status` | VARCHAR | OPEN, IN_PROGRESS, RESOLVED. |

---

## 📡 API Specification

### Authentication Endpoints
- `POST /api/auth/register`: Create a new account.
- `POST /api/auth/login`: Authenticate and receive a JWT.
- `GET /api/auth/profile`: Retrieve detailed user metadata (requires JWT).
- `POST /api/auth/google`: Handle OAuth2 callback tokens.

### Management Endpoints
- `GET /api/auth/users`: List all users (Admin only).
- `POST /api/auth/bug-reports`: Submit a new system issue.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core framework.
- **Spring Security**: RBAC and JWT filter implementation.
- **Spring Data JPA**: Hibernate-based persistence.
- **MySQL**: Relational data storage.
- **Lombok**: Boilerplate reduction.
- **ModelMapper**: DTO conversion.

---

## 📈 Monitoring & Maintenance
- **Health Check**: `GET /actuator/health`
- **Metrics**: Integrated with Prometheus/Grafana via Micrometer.
- **Logs**: Centralized logging via Logback, observable through the Admin Server (:9090).

---

## 🔧 Configuration & Environment
Essential `.env` variables required for this service:
```bash
AUTH_DB_URL=jdbc:mysql://mysql-auth:3306/lms_auth
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_ultra_secure_secret
GOOGLE_CLIENT_ID=your_google_id
GOOGLE_CLIENT_SECRET=your_google_secret
```

---
© 2026 EduLearn Engineering. Identity Service Documentation v1.0.
