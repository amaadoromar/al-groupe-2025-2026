# eSante - Remote Patient Monitoring Platform

This repository contains a comprehensive e-health monitoring system for remote patient care.

## Project Structure

```
al-groupe-2025-2026/
├── frontend/              # Web UI for patients and healthcare providers
├── backend/               # Spring Boot microservices
│   ├── monitoring-service/
│   └── reporting-service/
├── infrastructure/        # Testing and development tools
│   ├── simulator/        # Go device simulator
│   ├── mosquitto/        # MQTT broker configuration
│   └── mqtt-ui/          # Web-based MQTT monitor
├── notification-service/  # Alert notification service
├── init/                 # Database initialization scripts
└── docs/                 # Project documentation
```

## Quick Start

### Frontend Demo (Standalone)

POC e-santé – Frontend (sans back)

Ce prototype front (HTML/CSS/JS sans dépendances) qui démontre la chaîne fonctionnelle demandée:

- Inscription d’un nouveau patient par un administrateur
- Simulation de capteurs (smartphone passerelle) pour 2–3 mesures: rythme cardiaque, SpO₂, température
- Tableau de bord pour visualiser les mesures en temps réel avec graphiques
- Système d’alertes (seuils critiques) avec notifications navigateur et son
- Génération d’un rapport imprimable/exportable en PDF (via impression navigateur)

Utilisation:
1) Ouvrir `frontend/index.html` dans un navigateur moderne (Chrome/Edge/Firefox).
2) Onglet Administration: créer un patient (au minimum prénom, nom, date de naissance).
3) Onglet Passerelle: sélectionner le patient, démarrer la simulation. Le bouton «Forcer alerte» introduit des valeurs critiques.
4) Onglet Tableau de bord: sélectionner le patient pour visualiser les courbes et les alertes. Bouton «Activer notifications» pour recevoir des notifications système.
5) Onglet Rapports: choisir patient et période, puis «Générer le rapport». Utiliser «Imprimer / Exporter en PDF» pour produire le PDF.

Notes techniques:
- Aucune dépendance externe; stockage local via `localStorage`.
- Les graphiques sont dessinés en `<canvas>` côté client.
- Seuils d'alerte: HR < 45 ou > 110, SpO₂ < 90, Température > 38.5°C (modifiables dans `frontend/app.js`).

### Backend Services

See `backend/` directory for Spring Boot microservices:
- Monitoring Service (Port 8081)
- Reporting Service (Port 8082)

```bash
docker compose up -d
```

### Infrastructure & Testing

Device simulator and MQTT broker for testing:

```bash
cd infrastructure
docker compose up -d
```

Access MQTT Web UI at http://localhost:8080

See [infrastructure/README.md](infrastructure/README.md) for detailed documentation.

## Components

- **Frontend**: Patient/provider web interface
- **Backend**: Spring Boot microservices (monitoring, reporting)
- **Simulator**: Go-based device/phone simulator with MQTT
- **MQTT Broker**: Mosquitto for message routing
- **Databases**: PostgreSQL (relational), InfluxDB (time-series)

## Team

- AMAADOR Omar
- ASCARI Yannick
- EL AJI Walid
- TLILI Abderrahmen
- NAJAR Sarra


