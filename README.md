# Cairo — Project Management Application

> A lightweight, self-hosted project management tool modeled on Jira's core workflows. Built with React 18, Spring Boot 4, and MySQL 8.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-green?style=flat-square)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)
![JWT](https://img.shields.io/badge/Auth-JWT-yellow?style=flat-square)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Database Setup](#database-setup)
- [Default Credentials](#default-credentials)
- [API Endpoints](#api-endpoints)
- [User Roles](#user-roles)
- [Environment Variables](#environment-variables)

---

## Overview

Cairo is a full-stack project management application that provides teams with a self-hosted alternative to enterprise tools like Jira. It supports project creation, sprint planning, Kanban board visualization, issue tracking, team collaboration via comments, and in-app notifications — all secured with JWT-based authentication and role-based access control.

---

## Features

- **Authentication** — Register and login with email/password. JWT tokens issued on success, expire after 24 hours
- **Projects** — Create projects with unique keys, add/remove team members
- **Sprints** — Create sprints, start them (PENDING → ACTIVE), complete them (moves unfinished issues to backlog)
- **Kanban Board** — 4-column board (To Do, In Progress, In Review, Done) for the active sprint
- **Issue Tracking** — Create issues with type (BUG/STORY/TASK/EPIC), priority, assignee, and story points. Filter by type, priority, status, and assignee
- **Comments** — Comment on issues. Authors and admins can delete comments
- **Notifications** — In-app notifications for assignments, comments, and status changes
- **Admin Panel** — Promote or demote users between USER and ADMIN roles
- **Audit History** — Every issue status change is logged with old value, new value, and who made the change

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, React Router v6, Axios, CSS Modules |
| Backend | Java 21, Spring Boot 4, Spring Security, JPA/Hibernate |
| Authentication | JWT (JSON Web Tokens), BCrypt password hashing |
| Database | MySQL 8 |
| Build Tools | Maven (backend), npm/Vite (frontend) |
| Testing | Postman (API), JUnit (unit tests) |

---

## Project Structure

```
capstone/
├── cairo-backend/          # Spring Boot application
│   ├── src/main/java/com/cairo/cairobackend/
│   │   ├── config/         # SecurityConfig
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Request and Response DTOs
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── security/       # JwtService, JwtAuthFilter, UserDetailsServiceImpl
│   │   └── service/        # Business logic
│   └── src/main/resources/
│       └── application.properties
│
├── cairo-frontend/         # React application
│   ├── src/
│   │   ├── components/
│   │   │   ├── layout/     # AppLayout, Sidebar, Topbar, ProtectedRoute
│   │   │   └── ui/         # Button, Input, Modal, Badge, Spinner, Skeleton, Select
│   │   ├── context/        # AuthContext (useReducer)
│   │   ├── hooks/          # useApi
│   │   ├── pages/          # All page components
│   │   └── services/       # Axios API service layer
│   ├── .env
│   └── vite.config.js
│
└── database/
    ├── Cairo.sql           # Schema (run first)
    └── seed.sql            # Sample data (run second)
```

---

## Prerequisites

Make sure you have these installed before starting:

- **Java 21** — [Download](https://adoptium.net/)
- **Maven 3.8+** — included with IntelliJ or [download separately](https://maven.apache.org/)
- **MySQL 8** — [Download](https://dev.mysql.com/downloads/mysql/) or via MAMP/XAMPP
- **Node.js 18+** — [Download](https://nodejs.org/)
- **IntelliJ IDEA** (recommended for backend)
- **VS Code** (recommended for frontend)

---

## Database Setup

**Step 1 — Create the database**

Open MySQL Workbench (or any MySQL client) and run:

```sql
CREATE DATABASE cairo;
```

**Step 2 — Run the schema**

Open `database/Cairo.sql` and run it against the `cairo` database. This creates all 10 tables with proper constraints and indexes.

**Step 3 — Run the seed data**

Open `database/seed.sql` and run it. This creates 4 users, 3 projects, 4 sprints, 14 issues, 6 comments, and 5 notifications for testing.

> **Note:** If you want to start fresh at any point, run this first:
> ```sql
> SET FOREIGN_KEY_CHECKS = 0;
> TRUNCATE TABLE notifications; TRUNCATE TABLE issue_history;
> TRUNCATE TABLE issue_labels; TRUNCATE TABLE comments;
> TRUNCATE TABLE issues; TRUNCATE TABLE labels;
> TRUNCATE TABLE sprints; TRUNCATE TABLE project_members;
> TRUNCATE TABLE projects; TRUNCATE TABLE users;
> SET FOREIGN_KEY_CHECKS = 1;
> ```
> Then re-run `seed.sql`.

---

## Backend Setup

**Step 1 — Clone and open**

```bash
git clone https://github.com/Mariaa97-rodri/cairo.git
cd cairo/cairo-backend
```

Open the `cairo-backend` folder in IntelliJ IDEA.

**Step 2 — Configure `application.properties`**

Open `src/main/resources/application.properties` and update the database credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cairo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

app.jwt.secret=cairo-super-secret-key-change-this-in-production-min-256-bits
app.jwt.expiration-ms=86400000

server.port=8081
```

**Step 3 — Run the application**

In IntelliJ, click the **Run** button on `CairoBackendApplication.java`, or from the terminal:

```bash
./mvnw spring-boot:run
```

You should see:
```
Started CairoBackendApplication in X.XXX seconds
```

The backend is now running at `http://localhost:8081`.

**Verify it's working:**

```bash
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP"}
```

---

## Frontend Setup

**Step 1 — Navigate to the frontend folder**

```bash
cd cairo/cairo-frontend
```

**Step 2 — Install dependencies**

```bash
npm install
```

**Step 3 — Configure environment variables**

Create a `.env` file in the `cairo-frontend` root:

```env
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

**Step 4 — Start the development server**

```bash
npm run dev
```

The frontend is now running at `http://localhost:5173`.

Open your browser and navigate to `http://localhost:5173` — you should see the Cairo login page with the animated background.

---

## Default Credentials

The seed data creates the following accounts. All passwords are `password123`.

| Name | Email | Role |
|---|---|---|
| Admin User | admin@cairo.com | ADMIN |
| Maria Aguilar | maria@cairo.com | USER |
| John Dev | john@cairo.com | USER |
| Sara Engineer | sara@cairo.com | USER |

> **Tip:** Log in as `admin@cairo.com` first. The Admin Panel (visible in the sidebar) lets you promote any user to ADMIN.

---

## API Endpoints

All endpoints are prefixed with `/api/v1`. Protected endpoints require a `Bearer` JWT token in the `Authorization` header.

### Auth (public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive JWT |

### Users (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | List all users |
| GET | `/users/me` | Get current user |
| GET | `/users/{id}` | Get user by ID |
| PATCH | `/users/{id}/role` | Update role — ADMIN only |

### Projects (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/projects` | Get all projects for current user |
| POST | `/projects` | Create a new project |
| GET | `/projects/{id}` | Get project by ID |
| DELETE | `/projects/{id}` | Delete project — owner or ADMIN |
| POST | `/projects/{id}/members` | Add member — owner or ADMIN |
| DELETE | `/projects/{id}/members/{userId}` | Remove member — owner or ADMIN |

### Sprints (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/projects/{id}/sprints` | Get all sprints for project |
| POST | `/projects/{id}/sprints` | Create sprint |
| PATCH | `/sprints/{id}/start` | Start sprint (PENDING → ACTIVE) |
| PATCH | `/sprints/{id}/complete` | Complete sprint (ACTIVE → COMPLETED) |
| POST | `/sprints/{id}/issues` | Add issue to sprint |
| GET | `/projects/{id}/board` | Get Kanban board for active sprint |

### Issues (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/projects/{id}/issues` | Get issues with optional filters |
| POST | `/projects/{id}/issues` | Create issue |
| GET | `/issues/{id}` | Get issue by ID |
| PUT | `/issues/{id}` | Update issue |
| PATCH | `/issues/{id}/status` | Update issue status |
| DELETE | `/issues/{id}` | Delete issue — reporter or ADMIN |

### Comments (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/issues/{id}/comments` | Get all comments for issue |
| POST | `/issues/{id}/comments` | Add comment |
| DELETE | `/comments/{id}` | Delete comment — author or ADMIN |

### Notifications (protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/notifications` | Get unread notifications |
| GET | `/notifications/count` | Get unread count |
| PATCH | `/notifications/read-all` | Mark all as read |

---

## User Roles

| Permission | USER | ADMIN |
|---|---|---|
| Register / Login | ✅ | ✅ |
| Create projects | ✅ | ✅ |
| Add/remove members (own projects) | ✅ | ✅ |
| Add/remove members (any project) | ❌ | ✅ |
| Create/update issues | ✅ | ✅ |
| Delete own issues | ✅ | ✅ |
| Delete any issue | ❌ | ✅ |
| Delete own comments | ✅ | ✅ |
| Delete any comment | ❌ | ✅ |
| Delete own projects | ✅ | ✅ |
| Delete any project | ❌ | ✅ |
| Promote/demote users | ❌ | ✅ |
| Access Admin Panel | ❌ | ✅ |

---

## Environment Variables

### Backend (`application.properties`)

| Property | Description | Default |
|---|---|---|
| `server.port` | Port the backend runs on | `8081` |
| `spring.datasource.url` | MySQL connection URL | `jdbc:mysql://localhost:3306/cairo` |
| `spring.datasource.username` | MySQL username | `root` |
| `spring.datasource.password` | MySQL password | *(set yours)* |
| `app.jwt.secret` | Secret key for signing JWTs | *(change in production)* |
| `app.jwt.expiration-ms` | JWT expiry in milliseconds | `86400000` (24h) |

### Frontend (`.env`)

| Variable | Description | Default |
|---|---|---|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8081/api/v1` |

---

## Database Schema

Cairo uses a 10-table relational schema:

| Table | Purpose |
|---|---|
| `users` | User accounts and roles |
| `projects` | Top-level project containers |
| `project_members` | M:M join between users and projects |
| `sprints` | Time-boxed work cycles |
| `issues` | Core entity — tasks, bugs, stories, epics |
| `comments` | User comments on issues |
| `labels` | Reusable tags scoped to a project |
| `issue_labels` | M:M join between issues and labels |
| `issue_history` | Audit log of issue field changes |
| `notifications` | In-app alerts for assignments and comments |

---

## Running Tests

```bash
# Backend unit tests
cd cairo-backend
./mvnw test

# API integration tests
# Import Cairo_Backend_v2.postman_collection.json into Postman
# Run the collection against http://localhost:8081
```

---
Figma: https://www.figma.com/board/MdyMyU7igWzTctPbDwX1kS/Cairo?node-id=0-1&t=lbna9LnBbQXSNGnU-1
*Cairo — UCI 2123 Capstone Project · Maria Aguilar · 2026*