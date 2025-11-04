# Identity Service API

This document describes the Users & Roles (Identity) microservice that provides authentication (JWT) and basic user/patient/proche management. It is aligned with the existing Postgres schema created by `init/pg_init.sql` (`roles`, `utilisateurs`, `patients`, `proches`).

Base path: `/api`

## Security
- Auth: JWT (HS256) created by the identity service.
- Env vars:
  - `ID_JWT_SECRET` (required for production; default dev value in properties)
  - `ID_JWT_ISSUER` (default `eSante-identity`)
  - `ID_JWT_TTL_SECONDS` (default `3600`)
- Include header `Authorization: Bearer <token>` on protected endpoints.
- Roles (from DB `roles.nom`): `ADMIN`, `DOCTEUR`, `INFIRMIER`, `PATIENT`, `PROCHE` (friend/trusted contact).

## Auth

- POST `/api/auth/login`
  - Body (JSON): `{ "email": "...", "password": "..." }`
  - Response 200 (JSON):
    - `accessToken` (string, JWT)
    - `userId` (int), `email`, `nom`, `prenom`, `role`
    - `roles` (array)
    - `patientId` (int, when role is PATIENT)
    - `friendOfPatientId` (int, when role is PROCHE)
  - Errors: 400 `Invalid credentials`
  - Notes: legacy seeded users use plaintext passwords; new users are stored with BCrypt.

- GET `/api/auth/me`
  - Auth required. Returns current token claims and `sub` (email).

## Users (ADMIN)

- POST `/api/users`
  - Create a user with a role.
  - Body: `{ nom, prenom, email, password, role }`
  - Response 200: `{ id, nom, prenom, email, role }`

- GET `/api/users?role=ROLE`
  - List users, optionally filtered by role name.
  - Response 200: array of `{ id, nom, prenom, email, role }`

## Patients (ADMIN)

- POST `/api/patients`
  - Link/create a `patients` record for an existing `utilisateurs` row (the patient user).
  - Body: `{ utilisateurId, dateNaissance?, sexe?, tailleCm?, poidsKg?, pathologiePrincipale? }`
  - Response 201: patient id (int)

## Proches (Trusted Contacts) (ADMIN)

- POST `/api/proches`
  - Create a trusted contact (friend) and link to a patient.
  - Body: `{ utilisateurId, patientId, lien? }`
  - Response 201: proche id (int)

## Token Claims
- `sub`: user email
- `roles`: array of role names (e.g., `["PATIENT"]`)
- `patientId`: when role is PATIENT
- `friendOfPatientId`: when role is PROCHE

These claims allow other services to enforce access:
- PATIENT sees summaries/notifications for their `patientId`.
- PROCHE can view summaries/alerts for `friendOfPatientId`.
- CAREGIVER/ADMIN enforce full access as needed by policy (future integration).

## Errors
- Validation errors return 400 with `{ error: "validation_error", fields: { ... } }`.
- Not found: 404 `{ error: "not_found", message }`.
- Generic: 500 `{ error: "internal_error", message }`.

## Database Mapping
- `roles (id SERIAL, nom VARCHAR UNIQUE)`
- `utilisateurs (id SERIAL, nom, prenom, email UNIQUE, mot_de_passe, role_id)`
- `patients (id SERIAL, utilisateur_id UNIQUE, ...)`
- `proches (id SERIAL, utilisateur_id UNIQUE, patient_id, lien)`
- JPA is configured with `ddl-auto=validate` to avoid schema drift.

