# Progress Microservice | EduLearn Academic Tracking

The Progress Service is the brain of the student's academic journey. It handles the high-frequency tracking of learning activity, detects course completion milestones, and manages the issuance of verified digital certificates.

---

## 🏛️ Service Overview
This service is designed for low-latency writes and reliable state management. It bridges the gap between raw video viewing activity and formal academic achievements.

- **Port**: 8087
- **Persistence**: 
  - **Redis**: High-speed cache for real-time video timestamp tracking.
  - **MySQL (`lms_progress`)**: Persistent storage for completion records and certificates.
- **Key Responsibility**: Determining when a student has met the 100% curriculum requirement to unlock certification.

---

## 🚀 Key Features

### 1. Granular Progress Tracking
- **Second-by-Second**: Instead of just marking lessons as "done," this service accumulates the actual `watchedSeconds` for every lesson.
- **Auto-Completion**: Once the `watchedSeconds` crosses a threshold (usually 90-95% of lesson duration), the lesson is automatically flagged as `isCompleted`.

### 2. Certification Lifecycle
- **Real-Time Detection**: Upon marking the final lesson of a course as complete, the service triggers an internal milestone check.
- **Verified Issuance**: Generates a unique `verificationCode` for every certificate to prevent fraud.
- **Public Verification**: Provides a public endpoint where third parties (employers, LinkedIn) can verify the authenticity of a student's credentials.

---

## 💻 Frontend Experience (UI Extras)
The Progress Service provides the data for the most interactive parts of the React application:
- **Circular Progress Bars**: The dashboard uses data from `/api/progress/course` to render vibrant, SVG-based circular progress indicators for each enrolled course.
- **Dashboard Milestone Confetti**: When a certificate is issued, the frontend triggers a `canvas-confetti` animation based on the `201 Created` response from this service.
- **Glassmorphism Certificate Viewer**: The "My Learning" page features a premium, glass-textured modal that allows students to preview and download their certificates in a sleek, modern UI.
- **Real-Time Video Sync**: While watching lessons, the video player periodically "heartbeats" the current timestamp to the `/track` endpoint, ensuring progress is never lost even if the tab is closed.

---

## 📊 Database Schema (lms_progress)

### `progress` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique record ID. |
| `student_id` | BIGINT | Logical reference to the Student. |
| `course_id` | BIGINT | Logical reference to the Course. |
| `lesson_id` | BIGINT | Logical reference to the Lesson. |
| `watched_seconds`| INT | Accumulated time spent on the lesson. |
| `is_completed` | BOOLEAN | Completion status flag. |
| `updated_at` | TIMESTAMP | Last sync timestamp. |

### `certificates` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique certificate ID. |
| `student_id` | BIGINT | Recipient ID. |
| `course_id` | BIGINT | Subject Course ID. |
| `verification_code`| UUID/String | Globally unique verification hash. |
| `issued_at` | TIMESTAMP | Date of completion. |

---

## 📡 API Specification

### Progress Tracking
- `POST /api/progress/track`: Record viewing activity (Query params: studentId, lessonId, watchedSeconds).
- `POST /api/progress/complete`: Force-mark a lesson as finished.
- `GET /api/progress/course`: Get total completion percentage for a course.
- `GET /api/progress/student`: Retrieve full academic transcript for a student.

### Certification
- `POST /api/progress/certificates/issue`: Generate a new certificate (triggered on 100% completion).
- `GET /api/progress/certificates`: Retrieve existing certificate data.
- **Public**: `GET /api/progress/certificates/verify?code=...`: Non-authenticated verification endpoint.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core microservice framework.
- **Spring Data Redis**: High-performance transient storage.
- **Spring Data JPA**: Relational mapping for certificates.
- **RabbitMQ**: Listens for `payment.success` to initialize progress tracking for new students.
- **Lombok & ModelMapper**: Clean code utilities.

---

## 📈 Monitoring & Scalability
- **Distributed Cache**: Redis ensures that high-volume "heartbeat" requests from thousands of concurrent video viewers do not overwhelm the MySQL database.
- **Health Monitoring**: Integrated with Spring Boot Admin (:9090) for real-time memory and heap inspection.
- **Log Levels**: Debug logging enabled for certificate generation logic to ensure auditability.

---

## 🔧 Configuration
```bash
PROGRESS_DB_URL=jdbc:mysql://mysql-progress:3306/lms_progress
REDIS_HOST=lms-redis
RABBITMQ_HOST=lms-rabbitmq
```

---
© 2026 EduLearn Engineering. Academic Progress Service Documentation v1.0.
