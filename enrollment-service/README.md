# Enrollment Microservice | EduLearn Subscriber Lifecycle

The Enrollment Service is the gatekeeper of student access on the EduLearn platform. it manages the legal and functional relationship between a Student and a Course, ensuring that content access is granted only upon verified purchase or administrative approval.

---

## 🏛️ Service Overview
This service acts as the orchestration layer for "Who can access what." It is a low-latency, high-reliability service that triggers various events across the ecosystem when a student joins a new course.

- **Port**: 8085
- **Database**: `lms_enrollment` (MySQL 8.0)
- **Key Responsibility**: Managing the subscription state, enrollment dates, and granting/revoking course access permissions.

---

## 🚀 Key Features

### 1. Subscription Management
- **Instant Activation**: Integrates with RabbitMQ to listen for `payment.success` events from the Payment Service. Upon receiving an event, it automatically creates an enrollment record.
- **Trial / Free Access**: Supports "Free Enrollment" for preview courses or administrative grants.
- **Status Lifecycle**: Manages statuses such as `ACTIVE`, `COMPLETED`, `REVOKED`, or `REFUNDED`.

### 2. Enrollment Metadata
- Tracks the precise `enrolledAt` timestamp for analytics.
- **Completion Sync**: Works with the Progress Service to update the `status` to `COMPLETED` when a student reaches 100% progress.
- Supports expiration logic (Future Scope) for time-limited course access.

### 3. Service-to-Service Integration
- **Auth Sync**: Verifies user existence via the Auth Service before creating records.
- **Course Sync**: Fetches course pricing and validity from the Course Service during the enrollment initiation.

---

## 💻 Frontend Experience (Access & UI Extras)
The Enrollment Service determines what the student sees on their dashboard:
- **"My Learning" Dashboard**: The primary page for students (`MyLearning.jsx`) uses this service to list all active courses the student has purchased.
- **Dynamic "Buy Now" vs "Continue Learning"**: The course detail page queries this service to determine if the current user is already enrolled. If they are, it shows a "Continue" button; otherwise, it shows the "Buy" button.
- **Locked Content UI**: The sidebar in the lesson player displays "Locked" icons for modules where enrollment is missing or inactive.
- **Glassmorphism Enrollment Cards**: On the student dashboard, enrolled courses are displayed in premium, glass-textured cards with progress bars and "Last Viewed" timestamps.

---

## 📊 Database Schema (lms_enrollment)

### `enrollments` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique enrollment record ID. |
| `student_id` | BIGINT | Logical reference to the Student. |
| `course_id` | BIGINT | Logical reference to the Course. |
| `enrolled_at` | TIMESTAMP | Date access was granted. |
| `status` | VARCHAR | ACTIVE, COMPLETED, SUSPENDED. |
| `completion_date`| TIMESTAMP | Automatically set when progress hits 100%. |

---

## 📡 API Specification

### Student Access
- **`GET /api/enrollment/my-courses`**: Retrieve all courses for the authenticated student.
- **`GET /api/enrollment/check/{courseId}`**: Boolean check if the student is currently enrolled.
- `POST /api/enrollment/manual`: Administratively enroll a student (Admin only).

### Administrative Management
- `GET /api/enrollment/course/{courseId}`: List all students enrolled in a specific course (Instructor only).
- `DELETE /api/enrollment/{id}`: Revoke access (Admin/Refund cases).

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core microservice logic.
- **Spring Data JPA**: Persistence and relational management.
- **RabbitMQ**: The primary listener for `payment.success` events.
- **OpenFeign**: For synchronous checks against the Auth and Course services.
- **MySQL**: Relational storage for permanent enrollment audit trails.

---

## 📈 Event Orchestration (The "built" logic)
1. **Payment Verification**: `Payment Service` verifies a Razorpay signature.
2. **Event Broadcast**: `Payment Service` sends a message to RabbitMQ: `{ "studentId": 1, "courseId": 10 }`.
3. **Consumption**: `Enrollment Service` receives the message and creates an `ACTIVE` enrollment record.
4. **Result**: The Student immediately sees the course in their "My Learning" tab without a manual refresh.

---

## 🛡️ Security & Integrity
- **Double-Enrollment Prevention**: Logic prevents a student from paying for or enrolling in the same course twice.
- **JWT Integrity**: Ensures the student can only fetch their *own* enrollments by comparing the request ID with the JWT claims.

---

## 🔧 Configuration
```bash
ENROLLMENT_DB_URL=jdbc:mysql://mysql-enrollment:3306/lms_enrollment
RABBITMQ_HOST=lms-rabbitmq
EUREKA_ZONE=http://lms-eureka:8761/eureka/
```

---
© 2026 EduLearn Engineering. Enrollment & Lifecycle Service Documentation v1.0.
