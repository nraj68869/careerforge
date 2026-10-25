# CareerForge

A placement management platform. Students build profiles and apply to jobs, recruiters post jobs
and review applicants, and a placement officer tracks the whole pipeline.

## Features
- JWT login and logout (logout really revokes the token), password change that signs out every device
- Roles: Student, Recruiter (admin-approved), Placement Officer, and exactly one Admin (enforced in the database)
- Jobs with required skills and a minimum CGPA, plus a **match score** for every student/job pair
- Applications with a server-enforced status flow: Applied → Shortlisted → Selected / Rejected
- Student profiles with a **PDF resume upload** or a resume link
- Notifications (bell icon), CSV placement report, admin dashboard

## Security design
- Passwords hashed with BCrypt; all secrets live in `.env` (never committed)
- Every endpoint is authorised on the server; hiding a button is never the only protection
- Other people's data answers **404**, so nothing can be probed; login lockout after repeated failures
- Uploads: PDF only, 2 MB, content checked, stored under random names, served only through the API

## Tech
Spring Boot · Spring Security · JPA/Hibernate · PostgreSQL · React (Vite) · Recoil

## Run it locally
1. Install JDK 21+, Node.js LTS and PostgreSQL. Create an empty database.
2. `backend/.env.example` → copy to `backend/.env` and fill in your values
   (generate `JWT_SECRET` with `openssl rand -base64 32`).
3. Backend: `cd backend && ./mvnw spring-boot:run`
4. Frontend: `cd Frontend/careereforge && cp .env.example .env && npm install && npm run dev`
5. Open http://localhost:5173 and log in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` from your `.env`.

## Tests
`cd backend && ./mvnw test -Dtest="*Test"`

## Roadmap
Refresh tokens in HttpOnly cookies · Flyway migrations · Docker · interview scheduling