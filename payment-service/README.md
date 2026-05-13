# Payment Microservice | EduLearn Financial Engine

The Payment Service is the commerce backbone of the EduLearn platform. It manages all financial transactions for course purchases and integrates with global payment gateways to provide a secure and frictionless checkout experience.

---

## 🏛️ Service Overview
This service is built for extreme security and transactional integrity. It acts as the trusted bridge between the LMS platform and external financial institutions.

- **Port**: 8086
- **Database**: `lms_payment` (MySQL 8.0)
- **Key Responsibility**: Securely processing course purchases and notifying the ecosystem of successful transactions.

---

## 🚀 Key Features

### 1. Razorpay Integration (Premium Checkout)
- **Order Creation**: Dynamically initiates orders with the Razorpay API based on current course pricing.
- **Signature Verification**: Implements robust HMAC SHA256 verification of payment signatures to prevent "Man-in-the-Middle" or spoofing attacks.
- **Support for Multiple Methods**: Inherits support for Credit/Debit Cards, UPI, Netbanking, and Wallets through the Razorpay SDK.

### 2. Transaction Auditing
- Maintains a permanent, immutable record of every payment attempt (Success, Failed, Pending).
- Stores unique `paymentId` and `orderId` references for easy reconciliation and customer support.

### 3. Automated Invoicing (SMTP)
- **Direct Emailing**: Upon successful payment verification, this service communicates directly with the **SMTP Server** to send a detailed PDF receipt or HTML invoice to the student.
- **Service Decoupling**: While general notifications go through the Notif Service, financial receipts are handled here for maximum reliability.

### 4. Event-Driven Activation
- Uses **RabbitMQ** to broadcast the `payment.success` event.
- This event is consumed by the **Enrollment Service** and **Progress Service** to instantly unlock course content for the student without a manual refresh.

---

## 💻 Frontend Experience (Commerce Extras)
The Payment Service powers the high-stakes transactional UI in the React application:
- **Seamless Modal Integration**: Uses the `useRazorpay` hook to launch the payment gateway modal directly over the course page, maintaining the premium "Single Page App" feel.
- **Real-Time Payment Logic**: The frontend waits for the Razorpay callback, then immediately sends the signature to the `/verify` endpoint for final platform activation.
- **Glassmorphism Pricing Cards**: Course pricing and "Buy Now" sections feature sleek, blurred backgrounds with pulsing accents to drive user action.
- **Interactive Billing History**: Provides the data for the student's transaction ledger, showing clean, formatted dates and currency symbols.

---

## 📊 Database Schema (lms_payment)

### `payments` Table
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique transaction ID. |
| `student_id` | BIGINT | Logical reference to the Payer. |
| `course_id` | BIGINT | Logical reference to the Product. |
| `amount` | DOUBLE | Transaction value. |
| `currency` | VARCHAR | Usually INR/USD. |
| `status` | VARCHAR | SUCCESS, FAILED, PENDING. |
| `order_id` | VARCHAR (UK) | External Razorpay Order ID. |
| `payment_id` | VARCHAR (UK) | External Razorpay Payment ID. |

---

## 📡 API Specification

### Transactional Endpoints
- **`POST /api/payment/create-order`**: Initialize a new transaction. Returns a Razorpay `order_id`.
- **`POST /api/payment/verify`**: Verify the Razorpay signature and finalize the purchase.
- `GET /api/payment/history/{studentId}`: Retrieve all past purchases for a user.
- `POST /api/payment/refund/{paymentId}`: Initiate a refund (Admin only).

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service core.
- **Razorpay Java SDK**: Gateway integration.
- **Spring Boot Starter Mail**: For direct SMTP invoice delivery.
- **RabbitMQ**: For cross-service event orchestration.
- **MySQL**: Relational auditing store.

---

## 🔒 Security & Compliance
- **PCI-DSS Compliance**: The service never stores raw card details; all sensitive data is handled by the Razorpay secure environment.
- **HMAC Verification**: All gateway callbacks are cryptographically verified using a private secret to ensure the payload originated from Razorpay.

---

## 🔧 Configuration
```bash
PAYMENT_DB_URL=jdbc:mysql://mysql-payment:3306/lms_payment
RAZORPAY_KEY_ID=rzp_test_your_key
RAZORPAY_KEY_SECRET=your_ultra_secret_key
SMTP_HOST=smtp.gmail.com
```

---
© 2026 EduLearn Engineering. Payment & Commerce Service Documentation v1.0.
