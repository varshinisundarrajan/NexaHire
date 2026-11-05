# NexaHire – Enterprise Recruitment & Applicant Tracking System (ATS)

[![Java 21/25](https://img.shields.io/badge/Java-21%20%7C%2025-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-red.svg)](https://spring.io/projects/spring-security)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Proprietary-navy.svg)](#)

---

## 1. Project Overview

**NexaHire** is a full-featured, enterprise-grade Applicant Tracking System (ATS) and Recruitment Management Platform designed to resolve recruitment inefficiencies faced by fast-growing enterprises, recruiting agencies, and HR departments.

The platform streamlines the end-to-end talent lifecycle: requisition creation, multi-stage candidate pipeline tracking, transparent resume keyword scoring, interview panel scheduling with structured scorecards, recruitment funnel conversion telemetry, role-based access control, and an immutable security audit trail.

---

## 2. Problem Statement

Modern talent acquisition teams face several critical operational bottlenecks:
- **Fragmented Workflows**: Job postings, resumes, emails, and interview evaluations are scattered across disparate spreadsheets and inbox folders.
- **Opaque Candidate Screening**: Black-box or pseudo-AI screening claims often introduce bias or confusion without transparent explanations of why candidates were prioritized.
- **Pipeline Blindspots**: Hiring managers and recruiters lack visibility into where applicants stall in the hiring funnel, leading to delayed time-to-hire.
- **Lack of Governance & Security**: Absence of role boundaries and immutable audit trails poses compliance risks for enterprise talent teams.

**NexaHire** solves these challenges through a unified, secure, and transparent multi-role web platform.

---

## 3. Objectives

- Deliver an intuitive, role-adaptive dashboard experience for **Admin**, **HR Manager**, **Recruiter**, and **Candidate**.
- Automate the recruitment pipeline across five standard enterprise stages: `Applied` &rarr; `Screening` &rarr; `Shortlisted` &rarr; `Interview` &rarr; `Selected / Rejected`.
- Implement a **transparent keyword scoring engine** that provides explainable percentage matches and highlights matching vs. missing skill requirements.
- Standardize interview feedback collection through structured star ratings and hire/reject recommendations.
- Provide recruitment funnel analytics and departmental hiring distribution metrics.
- Maintain data integrity with enterprise audit logs and BCrypt-secured JWT authentication.

---

## 4. Technology Stack

### Frontend
- **HTML5 & Modern CSS3**: Enterprise design system featuring navy blue (`#0f172a`), slate accents, and clean card surfaces.
- **Vanilla JavaScript (ES6+)**: Modular API client, asynchronous fetch operations, dynamic role-based navigation, modal dialogs, and animated toast alerts. Zero heavy external frontend frameworks required.
- **Responsive Layout**: Fluid CSS Grid and Flexbox supporting desktop and mobile viewports.

### Backend
- **Java 21 / 25**: Enterprise language foundation.
- **Spring Boot 3.3.5**: Production-grade application framework.
- **Spring Web (MVC)**: RESTful controllers with standardized JSON API envelopes.
- **Spring Data JPA & Hibernate**: Entity-relationship mapping, pagination, and sorting.
- **Spring Security 6 & JJWT 0.12.6**: Stateless JWT authentication, role-based method security, and BCrypt password encryption.
- **Apache PDFBox 3.0.3**: Resume document text extraction for transparent keyword parsing.
- **Jakarta Bean Validation**: Robust server-side validation.

### Database & Infrastructure
- **MySQL 8.0**: Relational database engine with foreign key constraints, unique indexes, and cascade triggers.
- **Maven**: Build and dependency management.

---

## 5. System Architecture

```
+-----------------------------------------------------------------------------------+
|                            NexaHire Frontend (SPA/MPA)                            |
| HTML5 + Vanilla JS + CSS3 Design System (Navy/White Enterprise Theme)             |
| - Landing Page (index.html)     - Auth (login.html, register.html)               |
| - Role Dashboards (dashboard)   - Job Management (jobs.html)                     |
| - ATS Pipeline (applications)   - Candidate Search & Resumes (candidates.html)   |
| - Interview Schedule & Rating   - Funnel Analytics (analytics.html)              |
| - Admin & Audit Logs (users)    - Candidate Profile & Upload (profile.html)      |
+------------------------------------------+----------------------------------------+
                                           | REST APIs (JSON + JWT Bearer)
                                           v
+-----------------------------------------------------------------------------------+
|                        Spring Boot 3.3 Backend (Java 25)                          |
| - Security Layer: Spring Security, JWT Auth Filter, BCrypt, Role-Based Access     |
| - Controller Layer: Auth, Job, Candidate, Application, Interview, Analytics, User |
| - Service Layer: Business logic, keyword scoring, file handling, audit logger     |
| - Repository Layer: Spring Data JPA Repositories with custom queries & pagination |
| - Exception Handling: @RestControllerAdvice, standardized API error responses     |
+------------------------------------------+----------------------------------------+
                                           | JPA / Hibernate
                                           v
+-----------------------------------------------------------------------------------+
|                               MySQL 8.0 Database                                  |
| Tables: users, roles, candidates, recruiters, jobs, applications, resumes,        |
|         interviews, notifications, audit_logs                                     |
+-----------------------------------------------------------------------------------+
```

---

## 6. User Roles & Permission Matrix

| Feature / Module | ADMIN | HR MANAGER | RECRUITER | CANDIDATE |
| :--- | :---: | :---: | :---: | :---: |
| **User & Role Governance** | Full Access | View Only | No Access | No Access |
| **System Audit Logs** | Full Access | Full Access | No Access | No Access |
| **Recruitment Funnel Analytics** | Full Access | Full Access | Full Access | No Access |
| **Create & Edit Job Openings** | Full Access | Full Access | Full Access | No Access |
| **Candidate Sourcing Directory** | Full Access | Full Access | Full Access | No Access |
| **ATS Pipeline Management** | Full Access | Full Access | Full Access | No Access |
| **Schedule Interviews & Scorecards**| Full Access | Full Access | Full Access | View / Join Own |
| **Job Search & 1-Click Apply** | View Only | View Only | View Only | Full Access |
| **Candidate Profile & Resume** | View Only | View Only | View Only | Full Access |

---

## 7. Database Schema

The database design uses ten normalized, relational MySQL tables:

```
  +-------------+       +-------------+       +-------------------+
  |    roles    |<------|    users    |<------|    candidates     |
  +-------------+       +-------------+       +-------------------+
                               |                        |
                               v                        v
                        +-------------+       +-------------------+
                        | recruiters  |       |      resumes      |
                        +-------------+       +-------------------+
                               |                        |
                               v                        v
                        +-------------+       +-------------------+
                        |    jobs     |<------|   applications    |
                        +-------------+       +-------------------+
                                                        |
                                                        v
                                              +-------------------+
                                              |    interviews     |
                                              +-------------------+
                                              |   notifications   |
                                              +-------------------+
                                              |    audit_logs     |
                                              +-------------------+
```

1. **`roles`**: Primary key `id`, unique `name` (`ROLE_ADMIN`, `ROLE_HR_MANAGER`, `ROLE_RECRUITER`, `ROLE_CANDIDATE`).
2. **`users`**: Unique `email`, `username`, BCrypt-hashed `password`, `full_name`, `phone`, `role_id`, `status` (`ACTIVE`, `INACTIVE`, `SUSPENDED`).
3. **`candidates`**: `user_id` (1:1 with `users`), `headline`, `skills`, `experience_years`, `education`, `location`, `current_salary`, `expected_salary`.
4. **`recruiters`**: `user_id`, `department`, `designation`, `company_name`.
5. **`jobs`**: `title`, `department`, `job_type`, `location`, `salary_min`, `salary_max`, `required_skills`, `status` (`ACTIVE`, `CLOSED`, `DRAFT`), `deadline`.
6. **`resumes`**: `candidate_id`, `file_name`, `storage_path`, `parsed_text`, `uploaded_at`.
7. **`applications`**: `job_id`, `candidate_id`, `resume_id`, `status` (`APPLIED`, `SCREENING`, `SHORTLISTED`, `INTERVIEW`, `SELECTED`, `REJECTED`), `match_score`, `matched_skills`, `missing_skills`. Unique constraint on `(job_id, candidate_id)`.
8. **`interviews`**: `application_id`, `interviewer_id`, `interview_date`, `interview_time`, `interview_type`, `meeting_link`, `rating` (1-5), `recommendation` (`HIRE`, `HOLD`, `REJECT`), `status`.
9. **`notifications`**: `user_id`, `title`, `message`, `type`, `is_read`, `link`.
10. **`audit_logs`**: `user_id`, `action`, `entity_type`, `entity_id`, `details`, `ip_address`, `timestamp`.

---

## 8. REST API Documentation

### Authentication (`/api/auth`)
- `POST /api/auth/register` - Create candidate or recruiter account
- `POST /api/auth/login` - Authenticate with credentials and retrieve JWT token
- `GET /api/auth/me` - Retrieve current session user
- `POST /api/auth/forgot-password` - Generate password reset token
- `POST /api/auth/reset-password` - Update password via valid reset token

### Jobs Requisitions (`/api/jobs`)
- `GET /api/jobs` - Search and filter job listings (supports pagination & sorting)
- `GET /api/jobs/{id}` - Retrieve job details
- `POST /api/jobs` - Create new requisition *(Recruiter, HR, Admin)*
- `PUT /api/jobs/{id}` - Modify existing opening *(Recruiter, HR, Admin)*
- `DELETE /api/jobs/{id}` - Remove opening *(HR, Admin)*
- `PATCH /api/jobs/{id}/status` - Update status to ACTIVE / CLOSED *(Staff)*

### Applications & ATS Pipeline (`/api/applications`)
- `POST /api/applications/apply` - Submit job application with instant keyword match scoring *(Candidate)*
- `GET /api/applications` - Search applications across all stages *(Staff)*
- `GET /api/applications/my` - Retrieve logged-in candidate's application history *(Candidate)*
- `GET /api/applications/pipeline/{jobId}` - Retrieve full pipeline for a specific requisition *(Staff)*
- `PATCH /api/applications/{id}/status` - Advance pipeline stage with recruiter notes *(Staff)*

### Candidate Management (`/api/candidates`)
- `GET /api/candidates` - Search talent directory by name, skills, and location *(Staff)*
- `GET /api/candidates/profile/me` - Retrieve candidate profile
- `PUT /api/candidates/profile/me` - Update profile headline, skills, and compensation
- `POST /api/candidates/resume` - Upload resume document with PDFBox keyword extraction *(Candidate)*

### Interviews & Scorecards (`/api/interviews`)
- `POST /api/interviews/schedule` - Schedule interview panel and dispatch alert *(Staff)*
- `GET /api/interviews/upcoming` - Retrieve upcoming interview schedule *(Staff)*
- `GET /api/interviews/my` - Retrieve candidate's scheduled interviews *(Candidate)*
- `PUT /api/interviews/{id}/feedback` - Submit evaluation rating and recommendation *(Staff)*

### Analytics & System Governance (`/api/analytics`, `/api/users`)
- `GET /api/analytics/summary` - Recruitment funnel progression and department metrics
- `GET /api/users` - List all system users with role assignment *(Admin)*
- `PUT /api/users/{id}` - Modify user details, role, or status *(Admin)*
- `GET /api/users/audit-logs` - Query immutable audit trails *(Admin, HR)*

---

## 9. Quick Demo Credentials

Pre-seeded accounts are immediately available out of the box:

| Role | Email | Password | Primary Purpose |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@nexahire.com` | `Admin@123` | System oversight, user permissions, audit logs |
| **HR MANAGER** | `hr@nexahire.com` | `Hr@123` | Funnel analytics, department hiring stats |
| **RECRUITER** | `recruiter@nexahire.com` | `Recruiter@123` | Post openings, ATS pipeline, schedule interviews |
| **CANDIDATE** | `candidate@nexahire.com` | `Candidate@123` | Browse jobs, 1-click apply, resume upload |

*Note: The login page includes convenient **1-Click Quick Demo Login** buttons.*

---

## 10. Installation & Local Setup

### Prerequisites
1. **Java JDK 21 or 25** installed (`java -version`).
2. **Apache Maven 3.8+** installed (`mvn -version`).
3. **MySQL Server 8.0+** running locally on port 3306.

### Step 1: Database Setup
Create the MySQL database and run the schema script:
```sql
CREATE DATABASE IF NOT EXISTS nexahire_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;
```
You can source the schema file via terminal:
```bash
mysql -u root -p nexahire_db < c:/NexaHire/database/schema.sql
```

### Step 2: Configure Database Credentials
Verify or update `c:\NexaHire\backend\src\main\resources\application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nexahire_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3: Build & Run the Backend
Open a terminal in `c:\NexaHire\backend\` and run:
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

The Spring Boot backend will start on **`http://localhost:8080`**.
On startup, `DataInitializer` will automatically seed the database with all 4 roles, test accounts, active jobs, pipeline applications, and scheduled interviews!

### Step 4: Access the Application
Open your web browser and navigate to:
```
http://localhost:8080/index.html
```
Or sign in directly:
```
http://localhost:8080/login.html
```

*Note: You can also open the standalone frontend files located under `c:\NexaHire\frontend\` directly in your browser or serve them with any static web server (such as Live Server or Python `http.server`).*

---

## 11. Screenshots & User Flow Highlights

- **Enterprise Landing Page**: Hero section with metric cards and capability breakdown.
- **Role-Aware Dashboard**: Tailored dashboards dynamically rendered based on the logged-in role.
- **ATS Kanban Board**: Visual multi-stage candidate movement (`Applied` &rarr; `Screening` &rarr; `Shortlisted` &rarr; `Interview` &rarr; `Selected` &rarr; `Rejected`).
- **Keyword Scoring Card**: Transparent matching score gauge showing matched vs. missing skills breakdown.
- **Interview Scheduling & Scorecards**: Calendar management, meeting links, and 5-star evaluation ratings.
- **Governance & Audit Trail**: Real-time audit log tracking actions, actors, and IP addresses.

---

## 12. Future Enhancements

- OAuth2 integration with Google / LinkedIn Sign-In.
- WebSocket / STOMP real-time live push notifications for pipeline stage updates.
- Automated interview calendar synchronization with Google Calendar & Microsoft Outlook.
- Candidate self-scheduling time slots via Cal.com / Calendly webhooks.
- Multi-tenant enterprise organization support.
