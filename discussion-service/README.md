# Discussion Microservice | EduLearn Social & Peer Learning

The Discussion Service is the social heart of the EduLearn platform. It enables real-time collaboration, peer-to-peer assistance, and direct student-to-instructor engagement through organized, lesson-specific discussion threads.

---

## 🏛️ Service Overview
This service manages the "Classroom Community" aspect of the LMS. It is designed for conversational data structures and high engagement between students and teachers.

- **Port**: 8088
- **Database**: `lms_discussion` (MySQL 8.0)
- **Key Responsibility**: Hosting threaded discussions, replies, and community-driven Q&A for every lesson.

---

## 🚀 Key Features

### 1. Threaded Contextual Discussions
- **Lesson-Level Scoping**: Every discussion thread is logically linked to a specific lesson ID. This ensures that help is provided in the exact context of the learning material.
- **Hierarchical Replies**: Supports parent-child relationship for replies, allowing for structured conversations and nested help.

### 2. Instructor Moderation
- Instructors are automatically notified (via Notification Service) when a new thread is started in their course.
- **Top-Level Answers**: Instructors can flag specific replies as "Official Answers" or "Helpful" to guide students.

### 3. Community Engagement
- Supports "Likes" or "Upvotes" (Future Scope) to surface the most valuable questions and answers.
- **Rich Text Support**: Students can post code snippets, links, and formatted text to explain complex problems.

---

## 💻 Frontend Experience (Social & UI Extras)
The Discussion Service powers the interactive "Community" tab in the React application:
- **Real-Time Feed**: The discussion tab uses a sleek, card-based layout where threads are displayed with author avatars and timestamps.
- **Glassmorphism Chat Bubbles**: Replies are styled using semi-transparent glassmorphism backgrounds with distinct colors for the instructor (Indigo) and students (Slate).
- **Interactive Comment Box**: Features a premium text editor with real-time character counting and a "Post" button that uses `framer-motion` for a smooth entry animation.
- **Avatar Integration**: Pulls user profile pictures from the Auth Service via the Gateway to provide a human touch to every conversation.
- **Infinite Scrolling**: (Future Scope) Implemented for lessons with hundreds of active student threads.

---

## 📊 Database Schema (lms_discussion)

### `discussion_threads` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique thread ID. |
| `course_id` | BIGINT | Parent course reference. |
| `lesson_id` | BIGINT | Contextual lesson reference. |
| `author_id` | BIGINT | ID of the student/instructor who started it. |
| `title` | VARCHAR | Discussion headline. |
| `content` | TEXT | The primary question or post body. |
| `created_at` | TIMESTAMP | Creation date. |

### `replies` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique reply ID. |
| `thread_id` | BIGINT (FK) | Reference to the parent thread. |
| `author_id` | BIGINT | ID of the replier. |
| `content` | TEXT | The response body. |
| `is_instructor_reply`| BOOLEAN | Flag for distinct UI styling. |

---

## 📡 API Specification

### Conversations
- **`GET /api/discussion/lesson/{lessonId}`**: Retrieve all threads for a specific lesson.
- **`POST /api/discussion/thread`**: Start a new conversation.
- **`POST /api/discussion/reply`**: Respond to an existing thread.

### Discovery
- `GET /api/discussion/course/{courseId}`: View all activity for an entire course (Instructor view).
- `GET /api/discussion/my-activity`: View all threads/replies created by the current user.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service foundation.
- **Spring Data JPA**: Efficient management of threaded data.
- **MySQL**: Relational storage optimized for complex joins between threads and replies.
- **Lombok**: Boilerplate reduction.
- **Hibernate**: For parent-child relationship mapping.

---

## 🛡️ Security & Moderation
- **Role Verification**: Only students enrolled in a course (checked via Enrollment Service) can post threads or replies.
- **Ownership**: Users can only edit or delete their *own* posts. Instructors have global delete permissions for their course's threads.
- **Sanitization**: All HTML/Markdown is sanitized on the backend to prevent malicious script injection.

---

## 🔧 Configuration
```bash
DISCUSSION_DB_URL=jdbc:mysql://mysql-discussion:3306/lms_discussion
EUREKA_ZONE=http://lms-eureka:8761/eureka/
JWT_SECRET=your_secret_here
```

---
© 2026 EduLearn Engineering. Community & Discussion Service Documentation v1.0.
