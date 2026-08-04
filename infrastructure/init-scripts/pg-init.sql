-- Initialize multiple databases for Microservices and Keycloak

-- 1. Keycloak DB & User
CREATE USER keycloak WITH PASSWORD 'password';
CREATE DATABASE keycloak OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- 2. Microservice Databases
CREATE DATABASE order_service_db;
CREATE DATABASE payment_service_db;
CREATE DATABASE ticket_service_db;
CREATE DATABASE inventory_service_db;

GRANT ALL PRIVILEGES ON DATABASE order_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE ticket_service_db TO "user";
GRANT ALL PRIVILEGES ON DATABASE inventory_service_db TO "user";
