# Notification Service — Guide d’exécution et de test

Ce guide explique comment démarrer le backend Spring Boot, tester le service de notifications (multi‑canal) et consommer le flux temps réel (SSE) depuis le frontend.

## Prérequis
- Java (17 ou 22). Le projet est configuré sur Java 22 dans `backend/pom.xml`.
- Maven installé (`mvn -v`).
- Optionnel: je peux ajouter un Maven Wrapper (`mvnw`) et/ou passer le projet en Java 17 si besoin.

## Démarrage de l’application
```bash
cd backend
mvn spring-boot:run
# Application sur: http://localhost:8080
```
Par défaut, une base H2 en mémoire est utilisée (profil dev). La console H2 est activée.

## Endpoints principaux
- Créer une notification: `POST /api/notifications`
- Lister les notifications: `GET /api/notifications?recipientId=<id>[&status=SENT|PENDING|READ][&page=0&size=20]`
- Marquer comme lue: `PATCH /api/notifications/{id}/read`
- Flux SSE (temps réel): `GET /api/notifications/stream?recipientId=<id>`

## Test rapide (Windows PowerShell)
S’abonner au flux SSE (ouvrir dans un navigateur):
```
http://localhost:8080/api/notifications/stream?recipientId=user-1
```

Créer une notification:
```powershell
$b = @{
  recipientId = "user-1"
  title = "Test"
  content = "Hello"
  severity = "INFO"
  channels = @("IN_APP","EMAIL")
} | ConvertTo-Json

Invoke-RestMethod http://localhost:8080/api/notifications -Method Post -Body $b -ContentType "application/json"
```

Lister les notifications:
```powershell
Invoke-RestMethod "http://localhost:8080/api/notifications?recipientId=user-1"
```

Marquer comme lue (remplacer <id>):
```powershell
Invoke-RestMethod "http://localhost:8080/api/notifications/<id>/read" -Method Patch
```

## Exemples curl (alternative)
S’abonner au SSE:
```bash
curl -N "http://localhost:8080/api/notifications/stream?recipientId=user-1"
```

Créer une notification:
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId":"user-1",
    "title":"Test",
    "content":"Hello",
    "severity":"INFO",
    "channels":["IN_APP","EMAIL"]
  }'
```

Lister:
```bash
curl "http://localhost:8080/api/notifications?recipientId=user-1"
```

Marquer comme lue:
```bash
curl -X PATCH "http://localhost:8080/api/notifications/<id>/read"
```

## Intégration Frontend (SSE)
Exemple minimal d’abonnement au flux SSE (JS):
```js
const es = new EventSource("http://localhost:8080/api/notifications/stream?recipientId=user-1");
es.addEventListener("notification", (e) => {
  const data = JSON.parse(e.data);
  console.log("Notification:", data);
});
```

## Console H2 (base de dev)
- URL: `http://localhost:8080/h2-console`
- JDBC: `jdbc:h2:mem:esante`
- User: `sa` — Mot de passe: vide

## Utiliser PostgreSQL (optionnel)
Exporter ces variables d’environnement puis démarrer:
```bash
# Windows (PowerShell)
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/esante"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
cd backend; mvn spring-boot:run
```
Pensez à ajuster `spring.jpa.hibernate.ddl-auto` dans `backend/src/main/resources/application.yml` selon votre besoin (`validate`, `update`, `none`, …).

## Dépannage
- Maven introuvable: installez Maven ou demandez l’ajout du Maven Wrapper (mvnw).
- Version Java: le projet est en Java 22. Si vous préférez Java 17, changez `java.version` dans `backend/pom.xml` et assurez-vous que votre JDK correspond.
- CORS: autorisés par défaut pour `http://localhost:3000` et `http://localhost:5173`. Adaptez au besoin dans `org.esante.common.config/WebConfig`.

## Tests unitaires
```bash
cd backend
mvn test
```
Un test basique du service de notifications est inclus et s’exécute avec le contexte Spring Boot.

