# eSante E2E Data Streaming - Quick Start

This guide helps you start the complete end-to-end data streaming pipeline.

## Architecture

```
[Simulator] --> [MQTT Broker] --> [Node-RED] --> [InfluxDB]
                      |                |
                   [MQTT UI]    [Notifications]
```

## Quick Start

### 1. Configure Environment
```bash
cp .env.e2e .env
```

Edit `.env` if needed (optional):
- `NUM_PATIENTS=10` - Number of simulated patients
- `EMIT_INTERVAL_SECONDS=10` - How often vitals are sent
- `ALERT_INTERVAL_SECONDS=60` - How often alerts are generated

### 2. Start All Services
```bash
docker compose -f docker-compose-e2e.yml up -d
```

### 3. Access Services

| Service | URL | Description |
|---------|-----|-------------|
| MQTT UI | http://localhost:8080 | Monitor MQTT messages in real-time |
| Node-RED | http://localhost:1880/admin | View/edit data flows |
| InfluxDB | http://localhost:8086 | Query time-series data |

InfluxDB credentials (from `.env`):
- Username: `admin`
- Password: `adminpassword`
- Org: `esante`
- Bucket: `patient_vitals`

### 4. Verify It's Working

#### Check Logs
```bash
# All services
docker compose -f docker-compose-e2e.yml logs -f

# Specific service
docker compose -f docker-compose-e2e.yml logs -f node-red
docker compose -f docker-compose-e2e.yml logs -f simulator
```

#### Monitor MQTT Messages
```bash
# All vitals
docker exec esante_mqtt_broker mosquitto_sub -t "esante/patient/+/vitals/#" -v

# Notifications only
docker exec esante_mqtt_broker mosquitto_sub -t "esante/notifications/#" -v
```

#### Query InfluxDB
1. Open http://localhost:8086
2. Login with credentials from `.env`
3. Go to Data Explorer
4. Query the `patient_vitals` bucket

Example Flux query:
```flux
from(bucket: "patient_vitals")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "patient_vitals")
```

## Data Flow

### Normal Vitals Flow
1. Simulator emits vitals → MQTT topic `esante/patient/{id}/vitals/{type}`
2. Node-RED receives and checks alert conditions
3. If normal → Write to InfluxDB
4. Data available for queries and dashboards

### Alert Flow
1. Simulator emits vitals with HR > 150 or battery < 30%
2. Node-RED detects alert condition
3. Creates notification → MQTT topic `esante/notifications/patient/{id}`
4. Backend/frontend can subscribe to receive alerts

## Example Payloads

### Vital Message (MQTT Input)
```json
{
  "patientId": "patient-5",
  "deviceType": "watch",
  "measurementType": "HEART_RATE",
  "value": 165,
  "unit": "BPM",
  "timestamp": "2025-11-05T10:30:00Z",
  "metadata": {
    "battery": 25,
    "deviceId": "device-watch-5",
    "firmware": "v1.2.3",
    "quality": "poor"
  }
}
```

### Alert Notification (MQTT Output)
```json
{
  "patientId": "patient-5",
  "timestamp": "2025-11-05T10:30:00Z",
  "vitalType": "HEART_RATE",
  "value": 165,
  "unit": "BPM",
  "alertReason": "High heart rate: 165 BPM, Low battery: 25%",
  "deviceId": "device-watch-5",
  "battery": 25
}
```

## Testing Alert Conditions

The simulator automatically generates alerts based on `ALERT_INTERVAL_SECONDS`:
- Every 60 seconds (default), some patients emit abnormal vitals
- Heart rates > 150 BPM
- Battery levels < 30% (~30% chance during alert cycles)

To test more frequently:
```bash
ALERT_INTERVAL_SECONDS=20 docker compose -f docker-compose-e2e.yml up -d
```

## Stopping Services

```bash
# Stop but keep data
docker compose -f docker-compose-e2e.yml down

# Stop and remove all data
docker compose -f docker-compose-e2e.yml down -v
```

## Troubleshooting

### Node-RED not starting
Check permissions on `infrastructure/node-red/` directory:
```bash
sudo chown -R 1000:1000 infrastructure/node-red/
```

### InfluxDB connection errors
Verify InfluxDB is healthy:
```bash
docker compose -f docker-compose-e2e.yml ps influxdb
```

Wait for initialization (first run takes ~30 seconds).

### No notifications appearing
1. Check Node-RED logs for errors
2. Verify flows are deployed: http://localhost:1880/admin
3. Subscribe to MQTT notifications topic to confirm they're being sent

### Simulator not connecting
1. Check Mosquitto is running and healthy
2. Verify network connectivity: `docker compose -f docker-compose-e2e.yml ps`

## Corporate Network Setup

If behind a corporate proxy:
```bash
# 1. Copy certificate
cp ~/ocpamacaroot1.pem infrastructure/simulator/ocpamacaroot1.pem

# 2. Set dockerfile in .env
echo "SIMULATOR_DOCKERFILE=Dockerfile.corporate" >> .env

# 3. Rebuild and restart
docker compose -f docker-compose-e2e.yml up --build -d simulator
```

## Next Steps

1. **Backend Integration**: Subscribe to `esante/notifications/patient/#` in Java monitoring-service
2. **Frontend Dashboard**: Display real-time notifications and InfluxDB charts
3. **Production Hardening**: Enable MQTT authentication, secure InfluxDB tokens
4. **Scaling**: Increase `NUM_PATIENTS` for load testing

For more details, see [infrastructure/README.md](infrastructure/README.md)
