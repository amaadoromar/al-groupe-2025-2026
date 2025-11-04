# Infrastructure

This directory contains all infrastructure components for the eSante project testing and development.

## Structure

```
infrastructure/
├── docker-compose.yml          # Main compose file for simulator stack
├── simulator/                  # Go-based device & phone simulator
│   ├── main.go                # Simulator logic
│   ├── go.mod                 # Go dependencies
│   ├── Dockerfile             # Container build file
│   └── ocpamacaroot1.pem     # Corporate certificate
├── mosquitto/                  # MQTT broker configuration
│   ├── config/
│   │   └── mosquitto.conf    # Broker settings
│   ├── data/                  # Persistent data (generated)
│   └── log/                   # Broker logs (generated)
└── mqtt-ui/                    # Web-based MQTT monitor
    ├── index.html             # Single-page UI
    └── Dockerfile             # Nginx container

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

## Usage

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

## Notes

- The simulator requires corporate certificate (`ocpamacaroot1.pem`) for Go module downloads
- WebSocket connection uses port 9001 for browser-based MQTT clients
- All data is ephemeral unless volumes are configured
- For production use, enable MQTT authentication in mosquitto.conf
