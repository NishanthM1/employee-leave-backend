# Employee Leave Management Backend

A Spring Boot REST API for managing employees, departments, leave types, leave balances, and leave requests. The backend uses PostgreSQL, Spring Data JPA, BCrypt password hashing, JWT authentication, role-based authorization, validation, optimistic locking, and a global API error contract.

## Key Features

- Username/password login with JWT access tokens.
- BCrypt password hashing.
- Role-based access for `EMPLOYEE`, `MANAGER`, and `ADMIN`.
- Employee self-service profile, balance, leave application, request history, and cancellation.
- Manager team visibility and same-department request approval/rejection.
- Admin management of employees, departments, leave types, balances, and all requests.
- Inclusive leave-day calculation and balance enforcement.
- Pending/approved overlap prevention.
- Optimistic locking for leave requests and leave balances.
- Typed business exceptions with consistent `400`, `401`, `403`, `404`, and `409` responses.
- Cross-year leave rejection by design.

## Tech Stack

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security
- JSON Web Tokens via JJWT 0.12.6
- Spring Data JPA and Hibernate
- PostgreSQL
- Maven
- JUnit, Spring Boot Test, MockMvc, and Spring Security Test

## Architecture

```text
src/main/java/com/example/employee_leave_backend/
├── config/          Development-only data initialization
├── controller/      REST endpoints
├── dto/             Validated request and response models
├── entity/          JPA entities and relationships
├── exception/       API error model, typed exceptions, global handler
├── repository/      Spring Data JPA repositories
├── security/        JWT service/filter, user loading, security rules
└── service/         Authentication and leave-management business logic
```

The normal request flow is controller -> service -> repository/entity. Controllers accept DTOs and services enforce ownership, department, status, balance, and concurrency rules.

## User Roles

### EMPLOYEE

- View only their own employee profile, balances, and leave requests.
- Apply leave only for themselves.
- Cancel only their own `PENDING` leave requests.
- Cannot approve, reject, or use admin management endpoints.
- Deactivated employees cannot authenticate or use existing JWTs.

### MANAGER

- View employees and leave requests in their assigned department.
- Approve or reject requests from their department only.
- The reviewer is always the authenticated manager; it is never supplied by the client.
- Cannot use admin management endpoints.

### ADMIN

- Manage employees, departments, leave types, and leave balances.
- View all employees, balances, and leave requests.
- Approve or reject any pending leave request.

## Authentication and Authorization

1. Send username and password to `POST /api/auth/login`.
2. Spring Security authenticates the database user and checks that employee accounts are active.
3. The response contains a signed JWT with the username and role claim.
4. Send the token on protected requests:

```http
Authorization: Bearer <token>
```

5. The JWT filter verifies the signature and expiration, then loads authorities from the database user. A client-modified JWT role claim cannot grant additional privileges.

The JWT signing secret is supplied through `JWT_SECRET`. It is never stored in Java source code.

## Business Rules

- Leave application is allowed only for active employees and active leave types.
- `startDate` must be on or before `endDate`.
- Leave days are calculated inclusively: January 5 through January 7 is 3 days.
- Requested days cannot exceed the current remaining balance.
- `PENDING` and `APPROVED` overlapping requests are rejected.
- New requests start as `PENDING`.
- Only `PENDING` requests can be approved, rejected, or cancelled.
- Balance days are deducted only on approval.
- Rejected and cancelled requests do not deduct balance.
- Employees can cancel only their own pending requests.
- Cross-year leave requests are rejected.
- A leave balance is unique per employee, leave type, and year.
- `LeaveBalance` and `LeaveRequest` use JPA optimistic locking. Conflicts return `409 Conflict`.

Workflow:

```text
Apply -> PENDING -> APPROVED
								 -> REJECTED

PENDING -> CANCELLED  (employee owner only)
```

## Database

The main tables are:

- `users`: login username, BCrypt password, and role.
- `departments`: department name, status, and optional manager user.
- `employees`: employee profile, linked user, department, and active status.
- `leave_types`: leave category, yearly allocation, and status.
- `leave_balances`: employee/type/year allocation, used days, and `version`.
- `leave_requests`: dates, calculated days, status, reviewer, timestamps, and `version`.

Relationships include users-to-employees, departments-to-employees, departments-to-manager users, employees/types-to-balances, and employees/types/users-to-leave requests. The balance uniqueness constraint is `(employee_id, leave_type_id, year)`. Foreign keys are defined in `Database/Untitled.sql`.

## Setup

### Prerequisites

- JDK 25
- PostgreSQL
- A PostgreSQL database user with permission to create and update the application schema

### Create the database

```bash
createdb employee_leave_db
psql -h localhost -U <database-user> -d employee_leave_db \
	-f Database/Untitled.sql
```

For an existing installation, apply the version migration statements in `Database/Untitled.sql` before starting with schema validation enabled. The migration initializes existing version values to `0` and makes both version columns non-null.

### Configure local environment

From `employee_leave_backend`:

```bash
cp .env.example .env
```

Replace the placeholders in `.env`:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/employee_leave_db
DATABASE_USERNAME=your_database_user
DATABASE_PASSWORD=your_database_password
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters-long
FRONTEND_URL=http://localhost:3000
```

Generate a strong local JWT secret, for example:

```bash
openssl rand -base64 48
```

`.env` is ignored by Git. Never commit real database credentials or JWT secrets.

The default `JPA_DDL_AUTO` value is `validate`; Hibernate verifies the schema but does not modify it. Use `JPA_DDL_AUTO=update` only for intentional local development schema changes.

### Build, test, and run

```bash
./mvnw clean test
./mvnw spring-boot:run
```

The API listens on `http://localhost:8081`.

Development-only seed initialization can be enabled with:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The seed initializer is disabled unless the `dev` profile is active. Change any development seed credentials before using them outside local development.

## API Reference

All endpoints except login require a valid bearer token unless stated otherwise. Successful create/update operations currently return `200 OK` with the response DTO.

### Authentication

| Method | URL | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticate with `username` and `password`; returns `token`, `username`, and `role`. |

Request:

```json
{
	"username": "employee1",
	"password": "your-password"
}
```

### Employees

| Method | URL | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/employees/me` | Authenticated employee | Return the authenticated employee profile. |
| `GET` | `/api/employees/{id}` | Authenticated | Return a profile subject to employee ownership or manager department checks. |
| `GET` | `/api/employees` | `ADMIN`, `MANAGER` | Admin sees all employees; manager sees their team. |
| `GET` | `/api/employees/my-team` | `MANAGER` | Return employees in the manager's department. |
| `POST` | `/api/employees` | `ADMIN` | Create an employee, linked user, hashed password, and current-year active leave balances. |
| `PUT` | `/api/employees/{id}` | `ADMIN` | Update employee profile and department. |
| `PATCH` | `/api/employees/{id}/deactivate` | `ADMIN` | Mark the employee inactive; inactive employees cannot authenticate or apply leave. |

Create employee request fields: `username`, `password`, `employeeCode`, `name`, `email`, optional `phone`, `joiningDate`, and `departmentId`.

### Departments

| Method | URL | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/departments` | Authenticated | List departments. |
| `GET` | `/api/departments/{id}` | Authenticated | Return one department. |
| `GET` | `/api/departments/me` | Authenticated | Return the authenticated manager's department; intended for manager accounts. |
| `POST` | `/api/departments` | `ADMIN` | Create a department. Optional `managerId` must identify a manager user. |
| `PUT` | `/api/departments/{id}` | `ADMIN` | Update department name and optional manager. |
| `PATCH` | `/api/departments/{id}/deactivate` | `ADMIN` | Mark a department inactive. |

Request body:

```json
{
	"name": "Engineering",
	"managerId": 12
}
```

### Leave Types

| Method | URL | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/leave-types` | Authenticated | List leave types. |
| `GET` | `/api/leave-types/{id}` | Authenticated | Return one leave type. |
| `POST` | `/api/leave-types` | `ADMIN` | Create a leave type and current-year balances for active employees. |
| `PUT` | `/api/leave-types/{id}` | `ADMIN` | Update name and `yearlyDays`. |
| `PATCH` | `/api/leave-types/{id}/deactivate` | `ADMIN` | Mark a leave type inactive. |

Request body:

```json
{
	"name": "Annual Leave",
	"yearlyDays": 20
}
```

### Leave Balances

| Method | URL | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/leave-balances/me` | `EMPLOYEE` | Return the authenticated employee's balances. |
| `GET` | `/api/leave-balances` | `ADMIN` | Return all balances. |
| `GET` | `/api/leave-balances/{id}` | `ADMIN` | Return one balance. |
| `POST` | `/api/leave-balances` | `ADMIN` | Create a balance using query parameters. |

Create balance query parameters: `employeeId`, `leaveTypeId`, `year`, and `totalDays`.

```text
POST /api/leave-balances?employeeId=1&leaveTypeId=2&year=2026&totalDays=20
```

There is currently no balance update or delete endpoint.

### Leave Requests

| Method | URL | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/leave-requests` | Authenticated employee | Apply leave for the authenticated employee. |
| `GET` | `/api/leave-requests` | Authenticated | Employee sees own requests, manager sees department requests, admin sees all. |
| `GET` | `/api/leave-requests/{id}` | Authenticated | Return one request subject to ownership/department checks. |
| `PATCH` | `/api/leave-requests/{id}/cancel` | `EMPLOYEE` | Cancel the employee owner's pending request. |
| `PATCH` | `/api/leave-requests/{id}/approve` | `MANAGER`, `ADMIN` | Approve a pending request; reviewer comes from the authenticated user. |
| `PATCH` | `/api/leave-requests/{id}/reject` | `MANAGER`, `ADMIN` | Reject a pending request with `rejectionReason`. |

Apply request body:

```json
{
	"employeeId": 1,
	"leaveTypeId": 2,
	"startDate": "2026-04-10",
	"endDate": "2026-04-12",
	"reason": "Personal leave"
}
```

The server verifies that `employeeId` belongs to the authenticated employee and calculates `days` automatically. Rejection uses a query parameter:

```text
PATCH /api/leave-requests/42/reject?rejectionReason=Insufficient%20coverage
```

## Error Responses

Errors use the `ApiError` shape:

```json
{
	"status": 400,
	"message": "Start date cannot be after end date",
	"timestamp": "2026-09-25T12:00:00"
}
```

Typical statuses:

- `400 Bad Request`: invalid dates, inactive leave usage, insufficient balance, or validation failure.
- `401 Unauthorized`: missing, malformed, expired, invalid, or unknown-user JWT.
- `403 Forbidden`: ownership violation, role restriction, or manager cross-department access.
- `404 Not Found`: requested employee, department, leave type, balance, or leave request does not exist.
- `409 Conflict`: duplicate data, overlap conflict, database constraint conflict, or optimistic-lock conflict.

Unexpected server errors return a generic `500 Internal Server Error` message without SQL details or stack traces.

## Testing

The project contains 55 automated tests covering employee, manager, admin, JWT, authorization, error handling, deactivation, schema validation, and optimistic locking.

Run the complete suite with:

```bash
./mvnw clean test
```

## Security Notes

- JWT signing uses the `JWT_SECRET` environment variable.
- Database URL, username, and password are environment-configured.
- `.env` is ignored by Git; `.env.example` contains placeholders only.
- BCrypt is used for stored passwords.
- Inactive employees cannot log in or use existing JWTs.
- Manager request visibility and approval/rejection are restricted to the manager's department.
- Admin, manager, and employee endpoint restrictions are enforced by Spring Security and service-level ownership checks.
- JWT role claims are not trusted for privilege escalation; authorities are loaded from the database user.

## License and Project Metadata

This repository does not currently declare a license or publish metadata in `pom.xml`. Add project-specific licensing and ownership information before public distribution.
