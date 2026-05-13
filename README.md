# EduLearn | Microservices Learning Management System

A high-performance, distributed LMS platform built with Spring Boot, React, and a modern microservices architecture. Designed for global scale, interactive learning, and real-time student engagement.

---

## 🏗️ Global Architecture
The system follows a **Database Per Service** pattern with **Logical Joins** at the application level. Communication is handled via **Synchronous REST** (for critical paths) and **Asynchronous RabbitMQ** (for event-driven workflows).

- **Architecture Blueprint**: [View System Diagrams](docs/system_diagrams.html)
- **Monitoring**: Spring Boot Admin Dashboard (:9090)
- **Discovery**: Eureka Service Registry (:8761)
- **Gateway**: Spring Cloud Gateway (:8000)

---

## 📊 Data Schema (ER Diagram)
The following diagram illustrates the **Logical Relationships** between services. Solid lines represent hard Foreign Keys within a single database, while dashed lines represent logical ID references across service boundaries.

```mermaid
erDiagram
    %% AUTH SERVICE
    USER {
        long id PK
        string email
        string role
    }

    %% COURSE & LESSON
    COURSE {
        long id PK
        string title
        long instructorId
    }
    LESSON {
        long id PK
        long courseId FK
        string videoUrl
    }
    COURSE ||--o{ LESSON : "Owns"

    %% ENROLLMENT
    ENROLLMENT {
        long id PK
        long studentId
        long courseId
        string status
    }

    %% PROGRESS
    PROGRESS {
        long id PK
        long studentId
        long lessonId
        boolean isCompleted
    }
    CERTIFICATE {
        long id PK
        long studentId
        long courseId
        string verificationCode
    }

    %% PAYMENT
    PAYMENT {
        long id PK
        long studentId
        long courseId
        string transactionId
    }

    %% Logical Relationships (Cross-Service)
    USER ..o{ COURSE : "Author of"
    USER ..o{ ENROLLMENT : "Subscriber"
    COURSE ..o{ ENROLLMENT : "Product"
    LESSON ..o{ PROGRESS : "Tracking"
    ENROLLMENT ..o{ CERTIFICATE : "Unlocks"
    PAYMENT ..o{ ENROLLMENT : "Triggers"
```

---

## 🛠️ Microservices Breakdown

### 1. Auth Service (:8081)
- **Responsibility**: Identity and Access Management (IAM).
- **Persistence**: `lms_auth` (MySQL).
- **Key Features**: 
  - JWT-based authentication.
  - Role-Based Access Control (RBAC).
  - Google OAuth2 integration.
  - System bug reporting module.

### 2. Course Service (:8082)
- **Responsibility**: Catalog and curriculum management.
- **Persistence**: `lms_course` (MySQL).
- **Key Features**: 
  - Course CRUD for instructors.
  - Category and level management.
  - Student rating and review system.
  - Integration with Lesson Service for curriculum mapping.

### 3. Lesson Service (:8083)
- **Responsibility**: Content delivery and AI augmentation.
- **Persistence**: `lms_lesson` (MySQL).
- **Key Features**: 
  - Video content management.
  - **AI Lesson Summaries**: Uses Groq/LLM to generate automated notes.
  - Resource and attachment management.

### 4. Assessment Service (:8084)
- **Responsibility**: Academic evaluation and grading.
- **Persistence**: `lms_assessment` (MySQL).
- **Key Features**: 
  - Interactive quiz creation.
  - Adaptive question types.
  - Automated grading and attempt tracking.

### 5. Enrollment Service (:8085)
- **Responsibility**: Student-to-Course lifecycle management.
- **Persistence**: `lms_enrollment` (MySQL).
- **Key Features**: 
  - Course subscription management.
  - Transaction-triggered activation (via RabbitMQ).
  - Student enrollment history.

### 6. Payment Service (:8086)
- **Responsibility**: Financial transactions and billing.
- **Persistence**: `lms_payment` (MySQL).
- **Key Features**: 
  - **Razorpay Integration**: Secure order creation and signature verification.
  - Transaction receipts and PDF invoices via SMTP.
  - Async payment success event broadcasting.

### 7. Progress Service (:8087)
- **Responsibility**: Learning activity tracking and certification.
- **Persistence**: Redis (for real-time tracking) & `lms_progress` (MySQL for records).
- **Key Features**: 
  - **Granular Tracking**: Records watched seconds per lesson.
  - Automated 100% completion detection.
  - Verified PDF certificate issuance.

### 8. Discussion Service (:8088)
- **Responsibility**: Peer-to-peer social learning.
- **Persistence**: `lms_discussion` (MySQL).
- **Key Features**: 
  - Lesson-specific discussion threads.
  - Real-time Q&A between students and instructors.

### 9. Notification Service (:8089)
- **Responsibility**: Multi-channel messaging.
- **Persistence**: `lms_notification` (MySQL) & Redis (WebSockets).
- **Key Features**: 
  - Real-time push notifications via WebSockets.
  - Automated email alerts (Welcome, Purchase, Milestone) via SMTP.

---

## ⚙️ Infrastructure Components

| Component | Port | Technology | Purpose |
| :--- | :--- | :--- | :--- |
| **API Gateway** | 8000 | Spring Cloud Gateway | Entry point, Rate limiting, JWT validation. |
| **Eureka Server** | 8761 | Netflix Eureka | Service registration and discovery. |
| **Admin Server** | 9090 | Spring Boot Admin | Health monitoring and log management. |
| **RabbitMQ** | 5672 | RabbitMQ | Distributed event bus for async flows. |
| **Redis** | 6379 | Redis | Distributed caching and real-time state. |
| **MySQL** | 3306 | MySQL 8.0 | Relational storage (isolated schemas). |

---

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- Maven 3.8+

### Deployment (Docker Compose)
1. Configure the `.env` file with your credentials (DB, RabbitMQ, Razorpay, Groq).
2. Build and start all services:
   ```bash
   docker-compose up --build -d
   ```
3. Monitor the startup in Spring Boot Admin at `http://localhost:9090`.

---

## 🧪 Documentation & Testing
- **API Docs**: Swagger UI available at `http://localhost:<service-port>/swagger-ui.html`.
- **System Blueprint**: Open `docs/system_diagrams.html` in your browser for the full technical breakdown.
- **SonarQube**: Standard quality gates implemented for coverage and security.

---
© 2026 EduLearn Engineering. All rights reserved.
