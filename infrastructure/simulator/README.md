# eSanté Device & Phone Simulator

Go-based service that simulates medical devices (watch, BP monitor, glucometer, scale) and the mobile edge (phone) to emit realistic health vitals and publish them to MQTT.

## Features

- 🏥 **Realistic Vital Signs**: Heart Rate, SpO₂, Blood Pressure, Glucose, Weight, Steps
- 📱 **Edge Simulation**: Mimics phone aggregation with normalized units, UTC timestamps, and metadata
- 🚨 **Alert Generation**: Configurable abnormal value injection for testing alert pipelines
- 🔧 **Fully Configurable**: Patient count, emission rate, alert frequency via environment variables
- 🐳 **Docker-Ready**: Multi-stage build for minimal image size
- 📊 **MQTT Publishing**: Messages follow topic structure `esante/patient/{id}/vitals/{type}`

## Message Format

Each vital measurement is published as JSON:

```json
{
  "patientId": "patient-1",
  "deviceType": "watch",
  "measurementType": "HEART_RATE",
  "value": 78.5,
  "value2": null,
  "unit": "bpm",
  "timestamp": "2025-11-04T10:15:00Z",
  "metadata": {
    "battery": 85,
    "quality": "good",
    "deviceId": "device-watch-1",
    "firmware": "v1.2.3"
  }
}
```

## Vital Types Generated

| Type | Device | Unit | Normal Range | Alert Threshold |
|------|--------|------|--------------|-----------------|
| HEART_RATE | watch | bpm | 60-100 | <45 or >110 |
| SPO2 | watch | % | 95-100 | <85 |
| BLOOD_PRESSURE | bp_monitor | mmHg | 110-130 / 70-85 | <90/>160 (sys) |
| GLUCOSE | glucometer | mg/dL | 80-140 | <60 or >250 |
| WEIGHT | scale | kg | 60-90 | <50 or >120 |
| STEPS | watch | steps | 2000-8000 | N/A |

## MQTT Topics

Messages are published to:
```
esante/patient/{patientId}/vitals/{measurementType}
```

Examples:
- `esante/patient/1/vitals/HEART_RATE`
- `esante/patient/1/vitals/SPO2`
- `esante/patient/2/vitals/BLOOD_PRESSURE`

## Quick Start

### Using Docker Compose

```bash
# Start MQTT broker + simulator with default settings (10 patients)
docker compose -f docker-compose-simulator.yml up -d

# View logs
docker compose -f docker-compose-simulator.yml logs -f simulator

# Stop everything
docker compose -f docker-compose-simulator.yml down
```

### Custom Configuration

Create a `.env` file or set environment variables:

```bash
# Number of simulated patients
NUM_PATIENTS=20

# Emit vitals every 5 seconds
EMIT_INTERVAL_SECONDS=5

# Generate alert/abnormal values every 30 seconds
ALERT_INTERVAL_SECONDS=30

docker compose -f docker-compose-simulator.yml up -d
```

Or pass directly:

```bash
NUM_PATIENTS=50 EMIT_INTERVAL_SECONDS=3 ALERT_INTERVAL_SECONDS=45 \
  docker compose -f docker-compose-simulator.yml up -d
```

## Configuration Options

| Variable | Default | Description |
|----------|---------|-------------|
| `MQTT_BROKER` | mosquitto | MQTT broker hostname |
| `MQTT_PORT` | 1883 | MQTT broker port |
| `NUM_PATIENTS` | 10 | Number of patients to simulate |
| `EMIT_INTERVAL_SECONDS` | 10 | How often to emit vitals for all patients |
| `ALERT_INTERVAL_SECONDS` | 60 | Generate abnormal values every N seconds |

## Accessing MQTT

### MQTT Web UI
Open browser: `http://localhost:8080`
- Connect to: `ws://localhost:9001`
- Subscribe to: `esante/patient/#` or `esante/patient/1/vitals/#`

### Using mosquitto_sub (CLI)

```bash
# Subscribe to all patients
docker exec -it mqtt_broker mosquitto_sub -t "esante/patient/#" -v

# Subscribe to specific patient
docker exec -it mqtt_broker mosquitto_sub -t "esante/patient/1/vitals/#" -v

# Subscribe to specific vital type for all patients
docker exec -it mqtt_broker mosquitto_sub -t "esante/patient/+/vitals/HEART_RATE" -v
```

### Using MQTT Explorer
1. Download [MQTT Explorer](http://mqtt-explorer.com/)
2. Connect to `localhost:1883`
3. Browse topics: `esante/patient/...`

## Development

### Build locally

```bash
cd simulator
go mod download
go build -o simulator main.go
```

### Run locally (requires MQTT broker)

```bash
# Start just the MQTT broker
docker compose -f docker-compose-simulator.yml up mosquitto -d

# Run simulator
export MQTT_BROKER=localhost
export NUM_PATIENTS=5
./simulator
```

## Architecture

```
┌─────────────────┐
│   Simulator     │
│   (Go App)      │
│                 │
│ - Generates     │
│   vitals for    │
│   N patients    │
│ - Adds noise    │
│ - Injects       │
│   alerts        │
└────────┬────────┘
         │ MQTT Publish
         │ QoS 1
         ▼
┌─────────────────┐
│   Mosquitto     │
│  MQTT Broker    │
│                 │
│ Port 1883       │
│ WebSocket 9001  │
└─────────────────┘
```

## Testing Alert Generation

The simulator generates alerts based on `ALERT_INTERVAL_SECONDS`:

```bash
# Generate alerts every 15 seconds (for quick testing)
ALERT_INTERVAL_SECONDS=15 docker compose -f docker-compose-simulator.yml up simulator

# Watch for alerts in logs
docker compose -f docker-compose-simulator.yml logs -f simulator | grep "Alert cycle"
```

Alert values will be outside normal ranges (see table above).

## Logs

Simulator logs show:
- 🚀 Startup configuration
- ✅ MQTT connection status
- 📊 Iteration summaries (patients processed, time elapsed)
- ⚠️ Alert cycle notifications

```
🚀 Starting eSanté Device & Phone Simulator...
📊 Configuration:
   MQTT Broker: mosquitto:1883
   Number of Patients: 10
   Emit Interval: 10 seconds
   Alert Interval: every 60 seconds
✅ Connected to MQTT broker
📡 Starting vitals simulation...
⚠️  Alert cycle 6 - generating abnormal values
📊 Iteration 6 completed (10 patients) - Running for 1m0s
```

## Troubleshooting

### Simulator can't connect to MQTT

```bash
# Check if Mosquitto is running
docker compose -f docker-compose-simulator.yml ps

# Check Mosquitto logs
docker compose -f docker-compose-simulator.yml logs mosquitto
```

### No messages appearing

```bash
# Verify simulator is running
docker compose -f docker-compose-simulator.yml logs simulator

# Test MQTT broker directly
docker exec -it mqtt_broker mosquitto_pub -t "test" -m "hello"
docker exec -it mqtt_broker mosquitto_sub -t "test" -C 1
```

### Change configuration

```bash
# Stop services
docker compose -f docker-compose-simulator.yml down

# Restart with new config
NUM_PATIENTS=50 docker compose -f docker-compose-simulator.yml up -d
```

## Integration with Backend

To integrate with the monitoring service:
1. Add MQTT consumer to monitoring-service
2. Subscribe to `esante/patient/+/vitals/#`
3. Parse JSON payload
4. Call existing monitoring API endpoints

## License

Part of the eSanté project - Polytech Nice Sophia
