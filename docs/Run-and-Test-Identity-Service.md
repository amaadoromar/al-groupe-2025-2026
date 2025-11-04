# Identity Service — Run & Test Guide

This guide explains how to run and test the Users & Roles microservice that provides authentication (JWT) and user/patient/proche management on top of the existing Postgres schema.

## Prerequisites
- Java (JDK 21)
- Maven installed (`mvn -v`)
- A PostgreSQL instance initialized with the schema from `init/pg_init.sql`

## Start the service
```
cd backend/identity-service
mvn spring-boot:run
# Service runs on: http://localhost:8084
```

Environment overrides (PowerShell examples):
```
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/eSantePg"
$env:SPRING_DATASOURCE_USERNAME = "admin"
$env:SPRING_DATASOURCE_PASSWORD = "admin"
$env:ID_JWT_SECRET = "dev-secret-change-me"
```

JPA is set to `validate`, so the tables must already exist (created by your init SQL).

## Endpoints (quick test)

### Login (seeded admin)
Use a user from your seed data (`init/pg_init.sql`), e.g. `sarra.najar@example.com` / `pass`.

PowerShell:
```
$b = @{ email = "omar.amaador@example.com"; password = "pass" } | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/api/auth/login -Method Post -Body $b -ContentType "application/json"
```

curl:
```
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sarra.najar@example.com","password":"pass"}'
```

### Users (ADMIN)
```
$h = @{ Authorization = "Bearer <token>" }
$b = @{ nom='Doe'; prenom='John'; email='john.doe@example.com'; password='secret123'; role='PROCHE' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/api/users -Method Post -Headers $h -Body $b -ContentType "application/json"

Invoke-RestMethod http://localhost:8084/api/users -Headers $h
Invoke-RestMethod "http://localhost:8084/api/users?role=PROCHE" -Headers $h
```

### Patients (ADMIN)
```
$b = @{ utilisateurId=3; dateNaissance='1960-04-12'; sexe='F'; tailleCm=168; poidsKg=72.8; pathologiePrincipale='Hypertension' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/api/patients -Method Post -Headers $h -Body $b -ContentType "application/json"
```

### Proches (ADMIN)
```
$b = @{ utilisateurId=4; patientId=1; lien='fille' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/api/proches -Method Post -Headers $h -Body $b -ContentType "application/json"
```

### Who am I
```
Invoke-RestMethod http://localhost:8084/api/auth/me -Headers $h
```

## Claims & Access
- Tokens contain `roles` and, when applicable, `patientId` (PATIENT) or `friendOfPatientId` (PROCHE).
- Other services can enforce:
  - PATIENT/PROCHE: summary-only access; no raw metrics.
  - CAREGIVER/ADMIN: broader access by policy.

## Troubleshooting
- 400 Invalid credentials: verify email/password (seeded users use plaintext `pass`).
- Schema validation errors: ensure `init/pg_init.sql` was applied to the database.
- 401/403: include `Authorization: Bearer <token>` and ensure the user has `ADMIN` for admin endpoints.

