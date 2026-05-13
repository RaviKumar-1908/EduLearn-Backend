@echo off
title LMS - Development Infrastructure
color 0E

echo.
echo  +-----------------------------------------------------------+
2: echo  ^|       LMS DEVELOPMENT INFRASTRUCTURE                   ^|
echo  ^|    (MySQL, Redis, RabbitMQ, Eureka, Gateway)            ^|
echo  +-----------------------------------------------------------+
echo.

echo  [*] Starting infrastructure services...
docker compose up -d mysql redis rabbitmq eureka-server api-gateway

echo.
echo  [V] Infrastructure is UP!
echo  -------------------------------------------------------------
echo  [*] Eureka Dashboard: http://localhost:8761
echo  [*] API Gateway:      http://localhost:8000
echo  [*] RabbitMQ Management: http://localhost:15672 (guest/guest)
echo  -------------------------------------------------------------
echo.
echo  [i] You can now run your individual Spring Boot services 
echo      from your IDE or using 'mvn spring-boot:run'.
echo.
pause
