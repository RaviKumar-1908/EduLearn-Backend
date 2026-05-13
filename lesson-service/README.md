# Lesson Microservice | EduLearn Content & AI Engine

The Lesson Service is the core content delivery hub of the platform. It manages the modular curriculum structure and integrates cutting-edge Generative AI to enhance the student learning experience through automated summarization.

---

## 🏛️ Service Overview
This service is responsible for managing all video lessons, reading materials, and instructional assets. It bridges the gap between static content and interactive AI-driven learning aids.

- **Port**: 8083
- **Database**: `lms_lesson` (MySQL 8.0)
- **Key Responsibility**: Serving high-quality video content and generating intelligent lesson summaries via the Groq LLM API.

---

## 🚀 Key Features

### 1. Curriculum Architecture
- Supports deep nested structures: Course $\rightarrow$ Module $\rightarrow$ Lesson.
- **Dynamic Ordering**: Allows instructors to reorder lessons using an `orderIndex` without breaking the student flow.
- **Preview Mode**: Flag individual lessons as "Free Preview" to drive course conversions before enrollment.

### 2. AI-Powered Intelligence (Groq Integration)
- **Automated Summaries**: Integration with the **Groq API** (using Llama 3 or similar) to analyze lesson transcripts/descriptions.
- **Study Notes Generation**: Provides students with instant, high-quality bulleted notes for every lesson with a single click.
- **Context Awareness**: The AI takes the course level and category into account to tailor the complexity of the generated summaries.

### 3. Resource Management
- Support for multiple asset types: MP4 Videos, PDF Attachments, and External Links.
- **Secure Streaming**: Provides signed or secure URLs for content to ensure only enrolled students can access non-preview lessons.

---

## 💻 Frontend Experience (UI & AI Extras)
The Lesson Service powers the most "premium" aspects of the React application:
- **Floating AI Mascot**: The lesson page features a pulsing, animated AI mascot (built with `framer-motion`) that acts as the entry point for AI summarization.
- **Glassmorphism Study Notes**: AI-generated notes are rendered inside a blurred glass-textured container with indigo accents, providing a state-of-the-art reading experience.
- **Interactive Video Sidebar**: The sidebar dynamically fetches lesson lists and progress status, allowing for seamless navigation between modular content.
- **Breadcrumb Navigation**: Automatically generates contextual navigation paths (e.g., *Science > Physics > Quantum Mechanics*) to keep the student oriented.

---

## 📊 Database Schema (lms_lesson)

### `lessons` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique lesson ID. |
| `course_id` | BIGINT (FK) | Logical reference to the parent course. |
| `title` | VARCHAR | Lesson headline. |
| `description` | TEXT | Detailed content body. |
| `video_url` | VARCHAR | Stream URL (S3/Cloudinary/Vimeo). |
| `order_index` | INT | Sorting order within the course. |
| `is_preview` | BOOLEAN | If true, guest users can watch this. |
| `ai_summary` | LONGTEXT | Cached JSON/Markdown of the AI summary. |

---

## 📡 API Specification

### Content Delivery
- `GET /api/lessons/course/{courseId}`: Retrieve the full curriculum for a course.
- `GET /api/lessons/{id}`: Get detailed metadata and video URL for a specific lesson.
- `PUT /api/lessons/reorder`: Update the order of multiple lessons (Instructor only).

### AI Services
- **`GET /api/lessons/{id}/summary`**: Trigger or retrieve an AI-generated summary. 
  - *Internal logic*: Checks if a summary already exists in the DB to avoid redundant LLM API costs.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service core.
- **OpenFeign / WebClient**: For high-performance communication with the Groq API.
- **Groq SDK/API**: For high-speed LLM inference.
- **Spring Data JPA**: Persistence and relationship mapping.
- **RabbitMQ**: Listens for course updates to invalidate AI caches if the content changes.

---

## 📈 Performance & Scaling
- **Caching Strategy**: AI summaries are aggressively cached in MySQL to minimize latency and external API costs.
- **Asynchronous Processing**: Heavy AI generation tasks can be offloaded to background threads to ensure the UI remains responsive.

---

## 🔧 Configuration
```bash
LESSON_DB_URL=jdbc:mysql://mysql-lesson:3306/lms_lesson
GROQ_API_KEY=gsk_your_ultra_secure_groq_key
EUREKA_ZONE=http://lms-eureka:8761/eureka/
```

---
© 2026 EduLearn Engineering. Content & AI Service Documentation v1.0.
