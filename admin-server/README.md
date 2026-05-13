# Admin Server | EduLearn Centralized Monitoring & Ops

The Admin Server is the operational command center for the EduLearn platform. It provides a powerful, visual management interface for all microservices, allowing engineers and administrators to monitor system health, inspect logs, and manage configuration in real-time.

---

## 🏛️ Service Overview
This service integrates **Spring Boot Admin** with the entire microservice mesh. It acts as an observation deck, pulling data from the Spring Boot Actuator endpoints of every registered service.

- **Port**: 9090
- **Technology**: Spring Boot Admin Server
- **Key Responsibility**: Real-time observability, health monitoring, and remote administration of the distributed system.

---

## 🚀 Key Features

### 1. Comprehensive Health Monitoring
- **Real-Time Status**: Displays a "Green/Red" status for every service instance.
- **JVM Metrics**: Inspect heap usage, non-heap memory, garbage collection rates, and active thread counts.
- **System Info**: View disk space, CPU load, and OS-level details for each microservice.

### 2. Live Log Management
- **Remote Log Level Updates**: Change the logging level (e.g., from INFO to DEBUG) for any service *without restarting it*. This is critical for troubleshooting production issues on the fly.
- **Log Inspection**: View the live log stream directly from the web interface.

### 3. HTTP Trace & API Auditing
- View the history of HTTP requests handled by each service.
- Inspect request/response headers and processing times to identify bottlenecks in the API Gateway or individual services.

### 4. Configuration Insight
- View all environment variables, property files (`application.yml`), and configuration profiles active in each service.
- **Scheduled Tasks**: Monitor all `@Scheduled` tasks running across the system (e.g., daily cleanup jobs or reporting tasks).

---

## 💻 System Management (Extras)
While not user-facing, the Admin Server is the "Insurance Policy" for the platform:
- **Instant Alerts**: (Future Scope) Can be configured to send Slack or Email alerts if a service goes DOWN.
- **Visual Mapping**: Complements the `system_diagrams.html` by providing the *live* state of the architecture described in the documentation.
- **Heap Dumps**: Ability to trigger and download JVM heap dumps for deep memory leak analysis.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Service core.
- **Spring Boot Admin Server**: UI and administration logic.
- **Spring Boot Actuator**: The data provider on the client (microservice) side.
- **Netflix Eureka Client**: For automatic discovery of all microservices to be monitored.

---

## 📡 Management Interface
The dashboard is accessible at **`http://localhost:9090`**.
- **Wallboard View**: A high-level visual "grid" showing all services at once.
- **Detail View**: Deep dive into a specific instance for metrics, environment, and logs.
- **Journal**: A chronological record of all service status changes (e.g., "Auth Service went DOWN at 10:15 AM").

---

## 📈 Integration Logic
1. **Client Setup**: Every microservice (Auth, Course, etc.) includes the `spring-boot-admin-starter-client`.
2. **Discovery**: The Admin Server discovers the services via **Eureka**.
3. **Polling**: The Admin Server periodically polls the `/actuator` endpoints of each service to refresh the UI with live data.

---

## 🔧 Configuration
The Admin Server is designed to find everything automatically via Eureka:
```yaml
server:
  port: 9090
spring:
  boot:
    admin:
      discovery:
        ignored-services: [api-gateway] # Example of filtering
```

---
© 2026 EduLearn Engineering. System Administration & Monitoring v1.0.
