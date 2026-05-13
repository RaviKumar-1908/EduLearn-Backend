# Notification Microservice | EduLearn Communication Hub

The Notification Service is the voice of the EduLearn platform. It ensures that students and instructors remain engaged through a seamless blend of real-time WebSocket alerts and professional email communications via SMTP.

---

## 🏛️ Service Overview
This service is designed for high-availability messaging. It acts as an event consumer that translates internal system changes (like a course purchase or a new reply) into user-facing communications.

- **Port**: 8089
- **Persistence**: 
  - **MySQL (`lms_notification`)**: Permanent log of sent notifications and email audit trails.
  - **Redis**: Real-time state management for WebSocket sessions and unread counts.
- **Key Responsibility**: Delivery of multi-channel notifications (Push, Email) and maintaining the user's notification history.

---

## 🚀 Key Features

### 1. Multi-Channel Messaging
- **SMTP Email Delivery**: Integrated with external mail servers to send transactional emails (Welcome, Purchase Confirmation, Certificate Unlocked).
- **Real-Time WebSockets**: Provides instant "in-app" notifications for activities like discussion replies or course status changes.
- **Template System**: Uses dynamic HTML templates to ensure consistent branding across all outgoing emails.

### 2. Event-Driven Consumption
- Heavily relies on **RabbitMQ** to listen for events from across the ecosystem:
  - `auth.user.registered` $\rightarrow$ Send Welcome Email.
  - `payment.success` $\rightarrow$ Send Transaction Receipt (via Payment Service or here).
  - `discussion.reply` $\rightarrow$ Notify Thread Author.
  - `progress.milestone` $\rightarrow$ Send Certificate Unlock Alert.

### 3. Notification History
- Stores a detailed record of every notification sent to a user.
- **Read/Unread Status**: Allows students to track what they've seen and what needs attention.
- **Archive Management**: Users can dismiss or delete notifications from their history.

---

## 💻 Frontend Experience (Messaging & UI Extras)
The Notification Service powers the "Living" part of the React application:
- **Floating Notification Bell**: The `Navbar.jsx` features a pulsing bell icon with a real-time "Red Dot" badge for unread counts.
- **Toast Notifications**: Incoming WebSocket messages trigger premium, glass-textured "Toast" alerts that slide in from the top-right using `framer-motion`.
- **Dedicated Notification Center**: A full-page or sidebar view where students can read past alerts in a clean, chronologically organized list.
- **Branded Email Layouts**: All emails are rendered with the EduLearn design system, featuring responsive buttons and dark-mode friendly styling.

---

## 📊 Database Schema (lms_notification)

### `notifications` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique notification ID. |
| `user_id` | BIGINT | Recipient ID. |
| `title` | VARCHAR | Brief alert headline. |
| `message` | TEXT | Detailed alert body. |
| `is_read` | BOOLEAN | Read/Unread flag. |
| `type` | VARCHAR | EMAIL, PUSH, SYSTEM. |
| `created_at` | TIMESTAMP | Timestamp for sorting. |

### `email_logs` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique log entry. |
| `recipient` | VARCHAR | Target email address. |
| `subject` | VARCHAR | Email subject line. |
| `status` | VARCHAR | SENT, FAILED, RETRYING. |
| `sent_at` | TIMESTAMP | Delivery time. |

---

## 📡 API Specification

### Alerts & History
- **`GET /api/notification/my`**: Fetch current user's notification history.
- **`PUT /api/notification/{id}/read`**: Mark an alert as seen.
- `DELETE /api/notification/{id}`: Dismiss a notification.

### Internal Hooks
- `POST /api/notification/send`: (Internal only) Trigger a specific notification manually.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service foundation.
- **Spring Boot Starter Mail**: SMTP integration for email delivery.
- **Spring WebSocket / STOMP**: For real-time browser alerts.
- **RabbitMQ**: The core engine for receiving cross-service events.
- **Spring Data Redis**: For managing active socket connections.
- **MySQL**: Persistent auditing and history.

---

## 📈 Reliability & Performance
- **Asynchronous Mail**: Email delivery is performed in background threads to ensure RabbitMQ consumers aren't blocked by slow SMTP responses.
- **Dead Letter Queues**: Failed notifications are routed to a DLQ in RabbitMQ for retry or manual inspection.

---

## 🔧 Configuration
```bash
NOTIFICATION_DB_URL=jdbc:mysql://mysql-notification:3306/lms_notification
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_USERNAME=notifications@edulearn.com
REDIS_HOST=lms-redis
```

---
© 2026 EduLearn Engineering. Communication & Notification Service Documentation v1.0.
