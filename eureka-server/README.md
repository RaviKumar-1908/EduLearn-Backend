# Eureka Discovery Server | EduLearn Service Registry

The Eureka Server is the central nervous system of the EduLearn microservices ecosystem. It acts as a service registry where every component of the platform checks in and stays connected, enabling seamless, dynamic communication without hardcoded IP addresses.

---

## 🏛️ Service Overview
In a distributed environment where service instances can be created, destroyed, or moved at any time, Eureka provides the source of truth for "Who is online and where are they?".

- **Port**: 8761
- **Technology**: Netflix Eureka Server
- **Key Responsibility**: Service registration, health monitoring, and dynamic lookup for the API Gateway and internal service-to-service calls.

---

## 🚀 Key Features

### 1. Dynamic Service Registration
- As soon as a microservice (e.g., Auth, Course) starts up, it sends a REST call to Eureka to register its IP address, port, and status.
- **Auto-Deregistration**: If a service fails to send a "Heartbeat" for a configured period, Eureka automatically removes it from the registry, preventing other services from sending traffic to a dead instance.

### 2. Client-Side Load Balancing
- The **API Gateway** and **OpenFeign** clients query Eureka to get a list of all available instances of a service.
- This allows for seamless scaling—if you start 5 instances of the `lesson-service`, Eureka will track all 5 and the Gateway will load-balance traffic between them.

### 3. Health Monitoring UI
- Provides a built-in web dashboard at `http://localhost:8761`.
- **Visibility**: View registered instances, their status (UP/DOWN), general system status, and environment metadata (like Java version and system uptime).

---

## 💻 System Impact (Extras)
While not directly used by the frontend, the Eureka Server is the reason the system remains stable:
- **Zero-Downtime Deployments**: Since services are discovered dynamically, new versions can be spun up and old ones spun down without ever breaking the "Link" to the frontend.
- **Fault Tolerance**: If a physical server fails, Eureka helps the Gateway "failover" to a healthy instance on a different machine instantly.

---

## 🛠️ Technology Stack
- **Spring Boot 3.x**: Core foundation.
- **Spring Cloud Netflix Eureka Server**: The registry implementation.
- **Spring Security**: (Optional) For securing the registry dashboard.

---

## 📊 Internal Logic & Heartbeats
- **Registry Fetch**: Clients refresh their local copy of the registry every 30 seconds.
- **Lease Renewal**: Services send a heartbeat every 30 seconds to confirm they are still healthy.
- **Self-Preservation Mode**: If too many heartbeats are missed at once (network issue), Eureka enters a mode where it stops expiring instances to avoid "emptying the world."

---

## 📈 Monitoring & Maintenance
- **Dashboard**: Accessible at `http://localhost:8761` for manual inspection.
- **Actuator Enabled**: Provides metrics on registry size and renewal rates.

---

## 🔧 Configuration
The configuration for the Eureka server is minimal, as its primary job is to coordinate others:
```yaml
server:
  port: 8761
eureka:
  client:
    registerWithEureka: false # It doesn't register with itself
    fetchRegistry: false
```

---
© 2026 EduLearn Engineering. Service Discovery Documentation v1.0.
