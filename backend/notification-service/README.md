# Notification Service (AL 2025-2026)

A Spring Boot microservice that provides two notification channels: email notifications via REST API and real-time notifications via WebSocket. Built for distributed systems requiring instant user feedback and asynchronous email delivery.

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **Modern web browser** (for WebSocket support)

## How to Launch the Service

   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

   The service starts on `http://localhost:8080`

## How to Test the Service

### Using the Web Interface

1. Open `http://localhost:8080` in your browser
2. Check WebSocket connection status (should show ✅ Connected)
3. Use the forms to send emails or real-time notifications

### Using API Endpoints

**Send Email:**
```bash
POST http://localhost:8080/api/notifications/email/send
Content-Type: application/json

{
  "to": "recipient@example.com",
  "subject": "Test Subject",
  "message": "Test message"
}
```

**Send Real-time Notification:**
```bash
POST http://localhost:8080/api/notifications/realtime/send
Content-Type: application/json

{
  "type": "INFO",
  "title": "Test",
  "message": "Real-time notification test"
}
```

**Health Checks:**
- Email service: `GET http://localhost:8080/api/notifications/email/health`
- Real-time service: `GET http://localhost:8080/api/notifications/realtime/health`

## How It Works

### Protocols & Architecture

**Email Notifications:**
- **Protocol**: SMTP (Simple Mail Transfer Protocol)
- **Transport**: TLS on port 587
- **Flow**: REST API → EmailService → JavaMailSender → SMTP Server

**Real-time Notifications:**
- **Protocol**: STOMP over WebSocket (with SockJS fallback)
- **Broker**: In-memory simple broker
- **Flow**: REST API → RealtimeNotificationService → SimpMessagingTemplate → WebSocket → All subscribed clients

### Architecture Layers

1. **Controllers**: REST endpoints for receiving requests
2. **Services**: Business logic for email and WebSocket notifications
3. **Models**: DTOs for request/response data validation
4. **Config**: WebSocket/STOMP configuration

When an email is sent, the service also broadcasts a real-time notification to all connected clients, providing instant feedback.

## Troubleshooting

| Problem | Solution |
|---------|----------|
| **Email not sending** | 1. Verify Gmail app password (not regular password)<br>2. Enable 2FA on Gmail account<br>3. Check `application.yml` credentials<br>4. Review logs for authentication errors |
| **WebSocket not connecting** | 1. Ensure service is running on port 8080<br>2. Check browser console (F12) for errors<br>3. Verify firewall allows WebSocket connections<br>4. Try different browser (SockJS fallback may activate) |
| **Port 8080 already in use** | 1. Stop other applications using port 8080<br>2. Or change port in `application.yml`: `server.port: 8081` |
| **CORS errors in browser** | Controllers have `@CrossOrigin(origins = "*")` enabled.<br>For production, restrict to specific domains. |
| **Notifications not appearing** | 1. Check WebSocket connection status in UI<br>2. Open browser console to see WebSocket messages<br>3. Verify subscription to `/topic/notifications` |

## Technology Stack

- **Spring Boot 3.5.7** (Java 17)
- **Spring Web** (REST API)
- **Spring WebSocket** (STOMP messaging)
- **Spring Mail** (SMTP)
- **Lombok** (Code generation)
- **SockJS** (WebSocket fallback)
- **STOMP.js** (WebSocket client)

---

**Author**: Ascari Yannick (Projet AL 2025-2026)
**License**: MIT
