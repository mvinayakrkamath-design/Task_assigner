# CTAGS — Corporate Task Assignment and Grading System (Spring Boot)

**Phase 1 (10% implementation)**: project setup, full database design (JPA entities),
JWT login with role-based access, and basic task assignment and status tracking.

## Requirements

- Java 17 or newer
- Maven 3.9+ (or open the folder in IntelliJ IDEA / Eclipse STS, which bundle Maven)

## Run it

```bash
mvn spring-boot:run        # starts on http://localhost:8080 with an H2 database in ./data
mvn test                   # runs the 9 Phase 1 tests
```

To use MySQL instead of H2:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

(edit `src/main/resources/application-mysql.properties` for your username and password).

Demo logins are created on first start (password `Password@123`):
`admin@ctags.com`, `manager@ctags.com`, `employee@ctags.com`, `employee2@ctags.com`.

Try it: log in as the manager, assign a task, log out, then log in as `employee@ctags.com`
and press **Start**, then **Submit**.

## Project structure

```
src/main/java/com/ctags/
├── CtagsApplication.java        # entry point
├── model/                       # JPA entities: User, Department, Task, Submission, Grade, AuditLog
│                                #   + enums Role, TaskPriority, TaskStatus
├── repository/                  # Spring Data JPA repositories
├── dto/Dtos.java                # request and response records
├── security/                    # JwtService, JwtAuthFilter, SecurityConfig
├── service/                     # AuthService, TaskService, AuditService
├── web/                         # REST controllers + global error handler
└── config/DataSeeder.java       # demo data on first start
src/main/resources/
├── application.properties       # H2 (dev), JWT settings
├── application-mysql.properties # MySQL profile
└── static/index.html            # login page + role-based task dashboard
src/test/java/com/ctags/Phase1Test.java
```

## API (Phase 1)

| Method | Endpoint | Role | Purpose |
| --- | --- | --- | --- |
| POST | /api/auth/login | Any | Log in, returns JWT |
| POST | /api/auth/register | Admin | Create a user |
| GET | /api/auth/me | Any logged in | Current user |
| GET | /api/users | Admin, Manager | All users / own team |
| POST | /api/tasks | Manager, Admin | Assign a task |
| GET | /api/tasks | Any logged in | Tasks for the user's role (optional `?status=`) |
| GET | /api/tasks/{id} | Assigner, Assignee, Admin | One task |
| PATCH | /api/tasks/{id}/status | Employee | Assigned → In Progress → Submitted |
| POST | /api/tasks/{id}/grade | Manager, Admin | Grade a submitted task using four rubric scores (0–10) |
| GET | /api/tasks/{id}/grade | Task participants, Admin | View the task's grade and feedback |
| GET | /api/health | Public | Health check |

Send the token as `Authorization: Bearer <token>`. JSON keys are snake_case.

Example:

```bash
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"email":"manager@ctags.com","password":"Password@123"}'
```

## Grading module (additional implementation)

Managers can grade tasks assigned by them after an employee submits the task. Admins can
also grade submitted tasks. Each rubric category is scored from 0 to 10:

- Quality: 40% of the final score
- Timeliness: 30%
- Completeness: 20%
- Communication: 10%

The final percentage is calculated by `Grade.computeFinalScore`. A task is moved to
`GRADED` after a successful grade. A task can only be graded once. The dashboard displays
**Grade** for submitted tasks and **View grade** for graded tasks.

Example request:

```bash
curl -X POST http://localhost:8080/api/tasks/1/grade \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"quality":9,"timeliness":8,"completeness":10,"communication":8,"comments":"Good work; improve documentation."}'
```

Other planned enhancements include submission attachments, performance summaries, and
deadline reminders with `@Scheduled`.
