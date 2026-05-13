# API Gateway | EduLearn Intelligent Routing & Security

The API Gateway is the single entry point for all client-side interactions with the EduLearn ecosystem. It provides critical cross-cutting concerns including request routing, security enforcement, and rate limiting to ensure the platform remains stable and secure under high load.

---

## 🏛️ Service Overview
As a centralized "Reverse Proxy," the Gateway simplifies the frontend by providing a single URL (`localhost:8000`) that masks the complexity of the 9+ underlying microservices.

- **Port**: 8000
- **Technology**: Spring Cloud Gateway
- **Key Responsibility**: Intelligent load balancing, JWT validation, and preventing unauthorized access to the service mesh.

---

## 🚀 Key Features

### 1. Dynamic Request Routing
- Uses **Eureka Discovery** to locate service instances dynamically.
- **Path-Based Routing**:
  - `/api/auth/**` $\rightarrow$ Auth Service.
  - `/api/courses/**` $\rightarrow$ Course Service.
  - `/api/payment/**` $\rightarrow$ Payment Service.
  - And so on for all 9 core services.

### 2. Edge Security (JWT Filtering)
- **Global Auth Filter**: Intercepts every incoming request to check for a valid `Authorization: Bearer <JWT>` header.
- **Signature Validation**: Verifies the JWT signature against the system's `JWT_SECRET` before allowing the request to proceed to the destination service.
- **Context Injection**: Extracts user metadata (ID, Role) from the token and injects it into the internal headers for the target microservice to use.

### 3. Distributed Rate Limiting
- Integrates with **Redis** to implement a "Token Bucket" algorithm for rate limiting.
- Protects the system from Brute Force attacks and API abuse by limiting requests per IP or per User.

### 4. Cross-Origin Resource Sharing (CORS)
- Provides a centralized CORS configuration to allow the React Frontend (typically on port 3000/5173) to communicate safely with the backend services.

---

## 💻 Frontend Experience (System Extras)
The Gateway is invisible to the user but essential for the React application:
- **Unified Base URL**: The `api.js` configuration in the frontend points to a single base URL, simplifying networking logic.
- **Latency Reduction**: By handling JWT validation at the edge, invalid requests are rejected instantly without wasting resources on business microservices.
- **Standardized Error Responses**: Ensures that the frontend receives consistent JSON error objects (401 Unauthorized, 429 Too Many Requests) across all service failures.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service core.
- **Spring Cloud Gateway**: Reactive routing engine.
- **Spring Data Redis**: Backing store for rate limiting and session state.
- **Spring Security**: Filter chain and security configuration.
- **Netflix Eureka Client**: For service instance discovery.

---

## 🛡️ Routing Logic Examples
The Gateway configuration (`application.yml`) defines the following critical paths:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payment/**
```

---

## 📈 Monitoring & Performance
- **Actuator Enabled**: Observable through Spring Boot Admin (:9090).
- **Reactive Stack**: Built on Project Reactor and Netty for non-blocking I/O, allowing it to handle thousands of concurrent connections with minimal memory footprint.

---

## 🔧 Configuration
Essential `.env` variables for the Gateway:
```bash
EUREKA_ZONE=http://lms-eureka:8761/eureka/
REDIS_HOST=lms-redis
JWT_SECRET=your_ultra_secure_secret
```

---
© 2026 EduLearn Engineering. API Gateway Documentation v1.0.
