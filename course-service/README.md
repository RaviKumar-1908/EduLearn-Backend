# Course Microservice | EduLearn Catalog & Expert Authoring

The Course Service is the primary entry point for content discovery and curriculum management on the EduLearn platform. It serves as the master repository for course metadata, instructor assignments, and user-generated ratings, ensuring a high-quality educational catalog.

---

## 🏛️ Service Overview
This service manages the high-level entities of the platform. It is designed for high-read throughput (for students browsing the catalog) and consistent write operations (for instructors authoring new content).

- **Port**: 8082
- **Database**: `lms_course` (MySQL 8.0)
- **Key Responsibility**: Managing course metadata, categories, pricing, and social feedback (ratings/reviews).

---

## 🚀 Key Features

### 1. Advanced Catalog Management
- **Search & Filter**: Supports complex querying by category (Science, Tech, Business), difficulty level (Beginner, Intermediate, Expert), and price range.
- **Dynamic Pricing**: Instructors can update course prices, which are then synchronized with the Payment Service for checkout.
- **Metadata Richness**: Stores thumbnails, total durations, instructor bios, and learning objectives.

### 2. Social Feedback Loop (Rating System)
- **Verified Reviews**: Only students who have a valid enrollment (checked via Enrollment Service) can post ratings.
- **Aggregated Scoring**: Automatically re-calculates the average course rating whenever a new review is submitted.
- **Sentiment Capture**: Stores detailed textual feedback to help instructors improve their content.

### 3. Instructor Performance Tracking
- Tracks total student enrollment counts per course.
- Provides instructors with high-level views of their content's popularity and revenue potential (linked with Payment Service).

### 4. Curriculum Synchronization
- Works as the parent service for the **Lesson Service**. While the Course Service handles the "Container," the Lesson Service handles the "Content."
- Maintains the logical hierarchy required for the frontend to render the Course Overview and Sidebar.

---

## 💻 Frontend Experience (Catalog & UI Extras)
The Course Service powers the most visually intensive parts of the React application:
- **Dynamic Grid Layout**: Uses a responsive CSS Grid to render high-performance "Course Cards" with glassmorphism hover effects.
- **Framer Motion Transitions**: When clicking a course, the frontend uses shared element transitions to expand the card into a full course detail page.
- **Interactive Rating Stars**: Uses a custom React Star component that provides immediate visual feedback and calculates average scores in real-time.
- **Instructor Dashboard**: Provides a dedicated "Management Center" for instructors to view their course stats in clean, data-dense cards with custom SVG icons.
- **Course Detail Modal**: A premium UI component that displays objectives, syllabus, and reviews in a multi-tabbed glassmorphism interface.

---

## 📊 Database Schema (lms_course)

### `courses` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique course identifier. |
| `instructor_id`| BIGINT | Logical reference to the Author (Auth Service). |
| `title` | VARCHAR | Professional course title. |
| `description` | TEXT | Detailed marketing copy and syllabus summary. |
| `price` | DOUBLE | Current purchase price. |
| `category` | VARCHAR | Science, Tech, Business, Arts, etc. |
| `level` | VARCHAR | Beginner, Intermediate, Expert. |
| `thumbnail_url`| VARCHAR | CDN or S3 path for the cover image. |
| `avg_rating` | DOUBLE | Calculated average of all reviews. |

### `ratings` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique review ID. |
| `course_id` | BIGINT (FK) | Reference to the rated course. |
| `student_id` | BIGINT | Logical reference to the reviewer. |
| `stars` | INT | Integer score (1-5). |
| `comment` | TEXT | Textual feedback from the student. |

---

## 📡 API Specification

### Catalog Discovery
- `GET /api/courses`: Retrieve all published courses with pagination.
- `GET /api/courses/{id}`: Detailed course info (including ratings).
- `GET /api/courses/search`: Search courses by keyword or category.

### Authoring (Instructor Role)
- `POST /api/courses`: Create a new course draft.
- `PUT /api/courses/{id}`: Update course metadata or pricing.
- `DELETE /api/courses/{id}`: Archive a course.

### Feedback
- `POST /api/courses/{id}/rate`: Submit a new star rating and comment.
- `GET /api/courses/{id}/reviews`: Fetch all verified student reviews.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core microservice architecture.
- **Spring Data JPA**: Efficient ORM for catalog management.
- **Hibernate Search**: (Optional) For high-speed keyword matching.
- **Lombok**: Simplifies domain entity creation.
- **MySQL**: Relational storage for complex curriculum data.

---

## 🛡️ Service Security
- **JWT-Based RBAC**: Only users with the `INSTRUCTOR` role can access the POST/PUT/DELETE endpoints.
- **Ownership Verification**: Before updating a course, the service verifies that the `instructor_id` matches the ID in the JWT.
- **Input Sanitization**: All descriptions and titles are sanitized to prevent XSS attacks in the frontend.

---

## 📈 Monitoring & Maintenance
- **Actuator Enabled**: Integrated with Prometheus and Grafana.
- **Centralized Registry**: Registers with Eureka for service-to-service communication.
- **Caching**: Utilizes a cache for `GET /api/courses` to ensure sub-100ms response times for the home page.

---

## 🔧 Configuration
```bash
COURSE_DB_URL=jdbc:mysql://mysql-course:3306/lms_course
EUREKA_ZONE=http://lms-eureka:8761/eureka/
JWT_SECRET=your_secret_here
```

---
© 2026 EduLearn Engineering. Course & Catalog Service Documentation v1.0.
