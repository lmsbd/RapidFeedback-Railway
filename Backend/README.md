# RFO Backend Project

Spring Boot + MyBatis + MySQL backend project for RFO.

## Technology Stack

- Java 17
- Spring Boot 3.5.5
- MyBatis 3.0.5
- MySQL 8.0
- Maven
- JUnit
- Default app port: `8076`

## Project Structure

```text
src/main/java/com/unimelb/swen90017/rfo/
├── common/
├── config/
├── controller/
├── service/
├── dao/
├── pojo/
├── util/
└── RfoApplication.java

src/main/resources/
├── mapper/
├── sql/
│   └── init.sql
└── application.properties
```

## Features

- Subject CRUD
- Subject name uniqueness check
- Pagination and search

## API Endpoints (Subject)

- `POST /api/subjects/create`
- `POST /api/subjects/update`
- `POST /api/subjects/delete`
- `POST /api/subjects/get`
- `POST /api/subjects/search`
- `POST /api/subjects/list`
- `POST /api/subjects/batch-delete`

## Run with Docker (Recommended)

### Prerequisites

- Docker Desktop (Windows/macOS) or Docker Engine + Compose (Linux)

### Quick Start

Run from the `Backend/src/main/resources/sql` directory:

```bash
docker-compose up -d
docker-compose ps
docker-compose logs -f mysql
```

Use `Ctrl+C` to stop log streaming (container keeps running).

### Daily Commands

```bash
# Start existing stopped container
docker-compose start

# Stop container (keep data)
docker-compose stop

# Remove container (keep volume data)
docker-compose down

# Rebuild after Dockerfile or docker-compose.yml changes
docker-compose up -d --build

# Full reset (remove container and volume data)
docker-compose down -v
docker-compose up -d
```

### Docker Database Connection

- Host: `localhost`
- Port: `3307`
- Database: `rfo_db`
- Root user: `root`
- Root password: `123456`
- App user: `rfo_user`
- App password: `rfo_password`

Connect inside container:

```bash
docker exec -it rfo-mysql mysql -u root -p
```

### Docker Application Configuration

Use this in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/rfo_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Australia/Melbourne&allowPublicKeyRetrieval=true
spring.datasource.username=rfo_user
spring.datasource.password=rfo_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### Docker Notes

- SQL scripts in `src/main/resources/sql` are mounted to `/docker-entrypoint-initdb.d`.
- Init scripts run only when MySQL data directory is empty (for example after `docker-compose down -v`).

### Docker Troubleshooting

- Container not starting: `docker-compose logs mysql`
- Port conflict: make sure `docker-compose.yml` uses `3307:3306`
- Windows: ensure Docker Desktop is running
- Linux: ensure Docker daemon is running with `sudo systemctl start docker`

## Run with Local MySQL (Alternative)

### Prerequisites

- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### Database Setup

```sql
CREATE DATABASE rfo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p rfo_db < src/main/resources/sql/init.sql
```

### Local Application Configuration

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rfo_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Run Backend Service

```bash
mvn clean compile
mvn spring-boot:run
```

API base URL: `http://localhost:8076`

## API Testing Examples

### Create Subject

```bash
curl -X POST "http://localhost:8076/api/subjects/create" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mathematics",
    "description": "Advanced mathematics and calculus"
  }'
```

### List Subjects

```bash
curl -X POST "http://localhost:8076/api/subjects/list" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Search Subjects

```bash
curl -X POST "http://localhost:8076/api/subjects/search" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "math",
    "pageNum": 1,
    "pageSize": 10
  }'
```

## Development Notes

- Use unified response format and exception handling.
- Validate request parameters.
- Apply SQL injection protections and CORS configuration.

## License

MIT License
