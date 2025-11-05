# Infrastructure

This directory contains all infrastructure components for the eSante project testing and development.

## Structure

```
infrastructure/
├── docker-compose.yml          # Main compose file for simulator stack
├── simulator/                  # Go-based device & phone simulator
│   ├── main.go                # Simulator logic
│   ├── go.mod                 # Go dependencies
│   ├── Dockerfile             # Container build file (standard)
│   ├── Dockerfile.corporate   # Container build file (with cert)
│   └── ocpamacaroot1.pem     # Corporate certificate
├── mosquitto/                  # MQTT broker configuration
│   ├── config/
│   │   └── mosquitto.conf    # Broker settings
│   ├── data/                  # Persistent data (generated)
│   └── log/                   # Broker logs (generated)
├── mqtt-ui/                    # Web-based MQTT monitor
│   ├── index.html             # Single-page UI
│   └── Dockerfile             # Nginx container
└── node-red/                   # Data streaming layer
    ├── flows.json             # Pre-configured flows
    ├── settings.js            # Node-RED configuration
    └── package.json           # Node dependencies

```

## Components

### 1. Device & Phone Simulator (Go)
- Simulates multiple patients with realistic health vitals
- Publishes to MQTT broker at configurable intervals
- Generates both normal and alert-level values
- Measurements: Heart Rate, SpO2, Blood Pressure, Glucose, Weight, Steps

### 2. MQTT Broker (Mosquitto)
- Eclipse Mosquitto 2.0
- Ports: 1883 (MQTT), 9001 (WebSocket)
- Configured for development (anonymous connections enabled)
- Persistent storage for messages and logs

### 3. MQTT Web UI
- Real-time monitoring dashboard
- WebSocket connection to Mosquitto
- Displays vitals with alert highlighting
- Patient and message statistics

### 4. Node-RED Data Streaming Layer
- Subscribes to patient vitals from MQTT
- Checks alert conditions (HR > 150, battery < 30%)
- Routes alerts to patient-specific notification queues
- Writes normal vitals to InfluxDB time-series database
- Pre-configured flows for immediate use

## Usage

### E2E Stack (Full System)

Run the complete end-to-end system from the root directory:

```bash
# Copy environment configuration
cp .env.e2e .env

# Start all services (simulator, MQTT, Node-RED, InfluxDB, UI)
docker compose -f docker-compose-e2e.yml up -d

# View logs
docker compose -f docker-compose-e2e.yml logs -f

# Stop services
docker compose -f docker-compose-e2e.yml down
```

Access points:
- MQTT UI: http://localhost:8080
- Node-RED: http://localhost:1880/admin
- InfluxDB: http://localhost:8086

### Infrastructure Stack Only (Testing)

### Start All Services
```bash
cd infrastructure
docker compose up -d
```

### View Logs
```bash
# Simulator logs
docker compose logs -f simulator

# MQTT broker logs
docker compose logs -f mosquitto

# All logs
docker compose logs -f
```

### Access Web UI
Open http://localhost:8080 in your browser

### Configuration

Create a `.env` file from the example:
```bash
cp .env.example .env
```

Environment variables:
- `NUM_PATIENTS` - Number of simulated patients (default: 10)
- `EMIT_INTERVAL_SECONDS` - Vitals emission interval (default: 10)
- `ALERT_INTERVAL_SECONDS` - Alert generation interval (default: 60)
- `SIMULATOR_DOCKERFILE` - Dockerfile to use (default: Dockerfile)

Example using inline environment variables:
```bash
NUM_PATIENTS=20 EMIT_INTERVAL_SECONDS=5 docker compose up -d
```

### Corporate Network Setup

If you're behind a corporate proxy with custom CA certificates:

1. Place your certificate file as `simulator/ocpamacaroot1.pem`
2. Set the dockerfile in your `.env`:
```bash
SIMULATOR_DOCKERFILE=Dockerfile.corporate
```
3. Build and run:
```bash
docker compose up --build -d
```

Without the certificate, use the default Dockerfile which works on standard networks.

### MQTT Topics

Format: `esante/patient/{patientId}/vitals/{type}`

Examples:
- `esante/patient/1/vitals/HEART_RATE`
- `esante/patient/2/vitals/SPO2`
- `esante/patient/3/vitals/BLOOD_PRESSURE`

### Stop Services
```bash
docker compose down
```

### Clean Up (including volumes)
```bash
docker compose down -v
```

## Development

### Rebuild Simulator
```bash
docker compose build simulator
docker compose up -d simulator
```

### Subscribe to MQTT from CLI
```bash
docker exec mqtt_broker mosquitto_sub -t "esante/#" -v
```

### Test MQTT Connection
```bash
docker exec mqtt_broker mosquitto_sub -t '$SYS/#' -C 1
```

## Node-RED Data Flow

The streaming layer processes data in three stages:

### 1. MQTT Input
- Subscribes to: `esante/patient/+/vitals/#`
- Receives all patient vital measurements
- QoS 1 for guaranteed delivery

### 2. Alert Detection
Checks conditions:
- **Heart Rate Alert**: value > 150 BPM
- **Battery Alert**: metadata.battery < 30%

### 3. Routing
- **Alerts**: Published to `esante/notifications/patient/{patientId}` (QoS 1)
- **Normal Vitals**: Written to InfluxDB `patient_vitals` bucket

### Notification Format
```json
{
  "patientId": "patient-1",
  "timestamp": "2025-11-05T10:30:00Z",
  "vitalType": "HEART_RATE",
  "value": 165,
  "unit": "BPM",
  "alertReason": "High heart rate: 165 BPM",
  "deviceId": "device-watch-1",
  "battery": 85
}
```

### InfluxDB Schema
- **Measurement**: `patient_vitals`
- **Tags**: patientId, deviceType, measurementType, deviceId
- **Fields**: value, value2 (for BP), battery, quality
- **Timestamp**: Nanosecond precision

## Monitoring Notifications

Subscribe to patient-specific notification queues:

```bash
# All notifications
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v

# Specific patient
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/patient/1" -v
```

## Notes

- The simulator generates low battery (10-35%) on ~30% of alert cycles
- Node-RED flows are pre-configured and ready to use
- InfluxDB data persists in Docker volumes
- Notifications are in-memory only (cleared on restart)
- For production use, enable MQTT authentication in mosquitto.conf
