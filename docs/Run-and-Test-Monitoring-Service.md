# Monitoring Service — Guide d’exécution et de test

Ce guide explique comment lancer les tests d’intégration du service de monitoring sans base de données externe. Tout s’exécute sur H2 en mémoire.

## Prérequis
- Java (JDK 21 ou compatible avec votre `JAVA_HOME`).
- Maven installé (`mvn -v`). Si vous ne l’avez pas, dites‑moi et j’ajouterai un Maven Wrapper.

## Lancer uniquement les tests
```
cd backend
mvn -q test
```
Les tests utilisent automatiquement H2 en mémoire (configurée dans `backend/src/main/resources/application.yml`). Aucune base PostgreSQL n’est requise.

## Ce que valident les tests
- Ingestion d’une mesure « normale » (aucun évènement généré).
- Détection d’alerte (SpO2 basse) → création d’un évènement et envoi d’une notification IN_APP.
- Listing des évènements pour un patient (statut OPEN).
- Acknowledgement d’un évènement (`PATCH /ack`) puis résolution (`PATCH /resolve`).
- Détection critique (TA élevée) → évènement CRITICAL + notification.
- Endpoint des métriques: dernier échantillon par type (`GET /metrics/latest`).

Le test d’intégration principal: `backend/src/test/java/org/esante/monitoring/MonitoringApiIntegrationTest.java`.

## Démarrer l’appli (optionnel)
```
cd backend
mvn spring-boot:run
# http://localhost:8080
```
Par défaut, la console H2 est accessible: `http://localhost:8080/h2-console` (JDBC: `jdbc:h2:mem:esante`, user `sa`, mot de passe vide).

## Dépannage
- Maven introuvable: installez Maven ou demandez l’ajout du Maven Wrapper.
- Version Java: le projet est configuré pour Java 21 dans le POM. Adaptez si nécessaire.
- Conflit de port: changez `server.port` dans `application.yml` ou via `-Dserver.port=...`.

