# Frontend Integration of Notification Service

This document summarizes the work to integrate the backend Notification Service into the static frontend (admin, gateway, dashboard, reports) via WebSocket/STOMP for real-time notifications.

## Overview

- Backend: Spring Boot microservice exposes a SockJS/STOMP endpoint at `GET /ws` and publishes messages to `/topic/notifications`.
- Frontend: Loads SockJS + STOMP libraries from CDN and opens a connection to receive real‑time notifications. Messages are displayed using the existing `toast` and browser Notification APIs.

## Backend Contract

- WebSocket/STOMP endpoint: `${BASE_URL}/ws`
- STOMP subscription topic: `/topic/notifications`
- Payload shape (JSON):
  - `type`: `INFO | SUCCESS | WARNING | ERROR`
  - `title`: string
  - `message`: string
  - `timestamp`: ISO date string

Cross‑origin is allowed by backend config (`@CrossOrigin("*")` and `setAllowedOriginPatterns("*")`).

## What Changed

Files updated/added to wire the realtime feed and display notifications:

- `frontend/js/common.js`: added `connectRealtimeNotifications(baseUrl, onMessage?)` which:
  - Creates a SockJS + STOMP client to `${baseUrl}/ws`.
  - Subscribes to `/topic/notifications`.
  - Converts incoming messages to UI toasts and Browser Notifications.
  - Reconnects automatically on connection loss (3s backoff).

- HTML pages now load SockJS/STOMP and define default base URL:
  - `frontend/admin.html`
  - `frontend/dashboard.html`
  - `frontend/gateway.html`
  - `frontend/reports.html`

  Each includes:

  ```html
  <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
  <script>window.NOTIF_BASE_URL = window.NOTIF_BASE_URL || 'http://localhost:8080';</script>
  ```

- Page modules initiate the connection at startup:
  - `frontend/js/admin.js`
  - `frontend/js/dashboard.js`
  - `frontend/js/gateway.js`
  - `frontend/js/reports.js`

  Each calls:

  ```js
  import { connectRealtimeNotifications } from './common.js';
  connectRealtimeNotifications(window.NOTIF_BASE_URL);
  ```

## How To Run

1. Start the backend Notification Service:
   - `cd backend/notification-service`
   - `./mvnw clean install && ./mvnw spring-boot:run`
   - Service listens on `http://localhost:8080`

2. Open any frontend page (`frontend/admin.html`, `frontend/dashboard.html`, etc.) in a modern browser.

3. You should see a toast "Connecté aux notifications" indicating the STOMP subscription is active.

4. Send a test realtime notification:
   - `POST http://localhost:8080/api/notifications/realtime/send`
   - Body:
     ```json
     { "type": "INFO", "title": "Test", "message": "Hello frontend" }
     ```
   - The message appears as a toast; if browser notifications are granted, a system notification appears as well.

## Configuration

- Default base URL is `http://localhost:8080`.
- Override by setting `window.NOTIF_BASE_URL` before modules load (e.g., inline script tag or via templating).

## Results & Behavior

- Realtime messages of type `ERROR` or `WARNING` show as critical toasts (with sound) and request system notification (if permitted).
- `INFO`/`SUCCESS` render as standard toasts.
- Auto‑reconnect handles transient backend restarts.

## Notes & Future Enhancements

- Connection indicator in the topbar (green/red dot) could be added for visibility.
- Scoped subscriptions (e.g., per user/patient) can be supported by publishing to separate topics and subscribing accordingly.
- For production, restrict allowed origins in the backend and avoid public CDNs by bundling SockJS/STOMP locally or via your asset pipeline.

