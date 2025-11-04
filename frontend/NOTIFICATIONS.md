Notifications Integration (Backend-driven)

Overview
- Realtime notifications now flow through the backend `notification-service`.
- Frontend connects via WebSocket (SockJS + STOMP) to subscribe to `/topic/notifications`.
- Alerts triggered in the Gateway page are sent to the backend (REST), which broadcasts them to all connected clients.
- Frontend displays notifications received from the backend; no more direct, purely client-side toasts for alerts.

What changed
- Added `js/notifications.js`:
  - `connectNotifications()` sets up SockJS/STOMP and subscribes to `/topic/notifications`.
  - `displayNotification(n)` renders a toast (critical style for WARNING/ERROR) and optional browser notification.
  - `sendRealtimeNotification(payload)` POSTs to `POST /api/notifications/realtime/send`.
  - Auto-connects on `DOMContentLoaded` with a configurable base URL.
- Updated pages to load dependencies and connect:
  - `admin.html`, `dashboard.html`, `gateway.html`, `reports.html` now include CDN scripts for SockJS/STOMP and call `connectNotifications()` as a module.
- Gateway alert generation now delegates to backend:
  - `js/gateway.js`: when thresholds are crossed, it calls `sendRealtimeNotification(...)` and returns, instead of showing local toasts.
  - Local alert history in `localStorage` is kept for the dashboard list; visual toasts come from the backend stream.

Backend expectations
- `notification-service` exposes:
  - STOMP endpoint: `/ws` (SockJS enabled), topic: `/topic/notifications`.
  - REST: `POST /api/notifications/realtime/send` with payload `{ type, title, message }`.
  - REST: `GET  /api/notifications/realtime/test` for a test message.
- CORS and WebSocket origins are open (`*`) in the service.

Configuring the base URL
- Default: `http://localhost:8080`.
- Override options (in priority order):
  1) Define `window.NOTIF_BASE_URL` before `connectNotifications()` is called.
  2) Set `localStorage.setItem('notifBaseUrl', 'http://host:port')`.

How to test
1) Start the backend notification service on port 8080.
2) Open `frontend/gateway.html` and `frontend/dashboard.html` in a browser.
3) In Gateway, click “Démarrer” to simulate data; force an alert with “Forcer alerte”.
4) You should see a toast appear (coming from the WebSocket push) on all open pages where notifications are connected.
5) Alternatively, call the test endpoint in your browser devtools:
   `fetch('http://localhost:8080/api/notifications/realtime/test')` → should produce a toast.
6) For browser-level notifications, allow permission when prompted on the dashboard (or grant manually).

Notes
- This setup centralizes alerting through the backend while preserving local alert history for charts/lists.
- If you want to scope notifications to a given patient, extend the backend payload to include a `patientId` and adapt `displayNotification` accordingly.

