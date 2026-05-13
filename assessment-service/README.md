# Assessment Microservice | EduLearn Academic Evaluation

The Assessment Service is the academic validation engine of the platform. It manages the creation, delivery, and automated grading of quizzes, ensuring that student learning is measurable and performance is tracked with high precision.

---

## 🏛️ Service Overview
This service handles the most critical academic interactions on the platform. It provides a secure environment for testing student knowledge and determining if they have met the standards required for course completion.

- **Port**: 8084
- **Database**: `lms_assessment` (MySQL 8.0)
- **Key Responsibility**: Managing quizzes, questions, and attempt results across all courses.

---

## 🚀 Key Features

### 1. Robust Quiz Engine
- **Modular Quizzes**: Instructors can create multiple quizzes per course, typically placed at the end of modules or the entire course.
- **Passing Thresholds**: Supports configurable passing scores (e.g., 70%). Reaching this threshold is often a prerequisite for the **Progress Service** to issue a certificate.
- **Randomization**: (Future Scope) Ability to shuffle questions to prevent academic dishonesty.

### 2. Flexible Question Builder
- **Multiple Choice (MCQ)**: The primary question type with support for single or multiple correct answers.
- **True/False**: Simple binary validation for quick knowledge checks.
- **Detailed Explanations**: Instructors can provide rationale for correct/incorrect answers, shown to the student after the attempt.

### 3. Automated Grading Lifecycle
- **Real-Time Scoring**: As soon as a student submits an attempt, the service calculates the score, determines the pass/fail status, and persists the result.
- **Detailed Feedback**: Provides a question-by-question breakdown of the student's performance.

---

## 💻 Frontend Experience (Testing & UI Extras)
The Assessment Service powers the high-stakes "Exam Room" in the React application:
- **Interactive Quiz Interface**: The `Quiz.jsx` component provides a clean, focused UI with a timer and progress indicator.
- **Glassmorphism Result Cards**: After submission, results are displayed in a premium glass-textured card with vibrant success (Green) or retry (Amber) accents.
- **Pulsing Correct/Incorrect Indicators**: Uses `framer-motion` to animate the feedback for each question, making the review process engaging.
- **Result Confetti**: Triggers a `canvas-confetti` burst when a student passes a quiz for the first time.
- **Attempt History Tab**: A dedicated section where students can view all their past attempts and scores in a clean table.

---

## 📊 Database Schema (lms_assessment)

### `quizzes` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique quiz identifier. |
| `course_id` | BIGINT | Logical reference to the parent course. |
| `title` | VARCHAR | Quiz name (e.g., "Final Exam"). |
| `passing_score` | INT | Required score percentage. |

### `questions` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique question ID. |
| `quiz_id` | BIGINT (FK) | Parent quiz reference. |
| `content` | TEXT | The question text. |
| `type` | VARCHAR | MCQ, TRUE_FALSE. |
| `options` | JSON/TEXT | The possible answers. |
| `correct_answer`| VARCHAR | The hashed or plain correct choice. |

### `attempts` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique attempt record. |
| `student_id` | BIGINT | Logical reference to the test taker. |
| `quiz_id` | BIGINT | The quiz that was taken. |
| `score` | INT | Final percentage score. |
| `passed` | BOOLEAN | Success flag. |
| `attempted_at` | TIMESTAMP | Date of the test. |

---

## 📡 API Specification

### Student Testing
- **`GET /api/assessment/quiz/{id}`**: Fetch quiz questions (without answers) for the student.
- **`POST /api/assessment/submit`**: Submit a completed quiz for grading.
- `GET /api/assessment/my-results`: View history of all quiz attempts.

### Content Creation (Instructor)
- `POST /api/assessment/quiz`: Create a new quiz for a course.
- `POST /api/assessment/question`: Add a question to an existing quiz.
- `GET /api/assessment/course/{courseId}/stats`: View aggregate student performance for a course.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core service framework.
- **Spring Data JPA**: Persistence and complex relational querying.
- **MySQL**: Relational storage for high-integrity academic data.
- **Lombok**: Data class simplification.

---

## 🛡️ Security & Integrity
- **Anti-Tamper Scoring**: The frontend only sends the `optionSelected` IDs. The actual grading logic and correct answer validation happen exclusively on the server to prevent cheating.
- **Attempt Throttling**: (Future Scope) Ability to limit the number of attempts a student can make within a certain timeframe.

---

## 🔧 Configuration
```bash
ASSESSMENT_DB_URL=jdbc:mysql://mysql-assessment:3306/lms_assessment
EUREKA_ZONE=http://lms-eureka:8761/eureka/
```

---
© 2026 EduLearn Engineering. Academic Assessment Service Documentation v1.0.
